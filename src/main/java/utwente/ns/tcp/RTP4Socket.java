package utwente.ns.tcp;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.ip.HRP4Packet;
import utwente.ns.ip.IHRP4Socket;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static utwente.ns.tcp.RTP4Layer.ConnectionState;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Socket implements IReceiveListener, Closeable {
    RTP4Layer rtp4Layer;

    Map<RemoteHost, RTP4Connection> remoteHostRTP4ConnectionMap;

    private ReentrantLock stateLock = new ReentrantLock();
    private Condition stateChanged = stateLock.newCondition();

    private final IHRP4Socket ipSocket;

    private BlockingQueue<HRP4Packet> receivedSynQueue;

    private TCPBlock tcpBlock;


    RTP4Socket(IHRP4Socket ipSocket, RTP4Layer rtp4Layer) {
        this.ipSocket = ipSocket;
        this.rtp4Layer = rtp4Layer;
        ipSocket.addReceiveListener(this);
        state = ConnectionState.LISTEN;
        tcpBlock = new TCPBlock();
        receivedPacketQueue = new LinkedBlockingQueue<>();
        receivedSynQueue = new LinkedBlockingQueue<>(10);
        unacknowledgedQueue = new LinkedBlockingQueue<>();
        actionQueue = new LinkedBlockingQueue<>();
        listeningConnections = new LinkedList<>();
    }

    RTP4Connection accept(long timeout) throws IOException, TimeoutException {
        HRP4Packet synPacket;
        long timeStart = System.currentTimeMillis();
        while (true) {
            synPacket = receivedSynQueue.poll();
            if (synPacket != null) {
                break;
            }
            if (System.currentTimeMillis() - timeStart > timeout) {
                throw new TimeoutException("Got no connection request within timeout");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        RTP4Connection rtp4Connection = new RTP4Connection(synPacket.getSrcAddr(), synPacket.getSrcPort(), this);
        addConnection(new RemoteHost(synPacket.getSrcAddr(), synPacket.getSrcPort()), rtp4Connection);
        rtp4Layer.registerConnection(rtp4Connection);
        //Todo might remove ^
        return rtp4Connection;
    }

    RTP4Connection connect(String address, int port) throws IOException {
        RemoteHost remoteHost = new RemoteHost(Util.addressStringToInt(address), (short) port);
        RTP4Connection rtp4Connection = new RTP4Connection(remoteHost, this);
        addConnection(remoteHost, rtp4Connection);
        rtp4Layer.registerConnection(rtp4Connection);
        //Todo might remove ^
        return rtp4Connection;
    }

    private void addConnection(RemoteHost remoteHost, RTP4Connection connection){
        remoteHostRTP4ConnectionMap.put(remoteHost, connection);
    }

    void removeConnection(RemoteHost remoteHost){
        remoteHostRTP4ConnectionMap.remove(remoteHost);
    }

    @Override
    public void close() throws IOException {
        actionQueue.offer(RTP4Layer.ConnectionAction.CLOSE);
        try {
            stateLock.lock();
            while (state != ConnectionState.CLOSED && state != ConnectionState.TIME_WAIT) {
                stateChanged.await();
            }
            if (state == ConnectionState.CLOSED) {
                clear();
            }
        } catch (InterruptedException e) {
            throw new IOException();
        } finally {
            stateLock.unlock();
        }
    }

    void clear(){
        try {
            ipSocket.close();
        } catch (IOException ignored) {
        }
        switch (state) {
            case CLOSED:
                return;
            case TIME_WAIT:
                if (System.currentTimeMillis() - timeWaitStart > Config.getInstance().getMaxSegmentLife()) {
                    try {
                        stateLock.lock();
                        state = ConnectionState.CLOSED;
                    } finally {
                        stateLock.unlock();
                    }
                }
        }
    }

    private void receiveAcknowledge(RTP4Packet packet) {
        if (tcpBlock.registerReceivedAcknowledgeNumber(packet.getAckNum())) {
            AbstractMap.Entry<RTP4Packet, Long> entry;
            while (true) {
                entry = unacknowledgedQueue.peek();
                if (entry == null){
                    break;
                }
                if (entry.getKey().getSeqNum() + entry.getKey().getLength() > packet.getAckNum()) {
                    unacknowledgedQueue.offer(entry);
                } else {
                    RTP4Packet acknowledgedPacket = entry.getKey();
                    unacknowledgedQueue.remove();
                    switch(state) {
                        case SYN_ACCEPTED:
                        case SYN_SENT:
                            if (acknowledgedPacket.isSyn()) {
                                state = ConnectionState.ESTABLISHED;
                            }  else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedQueue but should not be");
                            }
                            break;
                        case FIN_WAIT_1:
                            if (acknowledgedPacket.isFin()) {
                                state = ConnectionState.FIN_WAIT_2;
                            }  else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedQueue but should not be");
                            }
                            break;
                        case CLOSING:
                            if (acknowledgedPacket.isFin()) {
                                state = ConnectionState.TIME_WAIT;
                                timeWaitStart = System.currentTimeMillis();
                            }  else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedQueue but should not be");
                            }
                            break;
                        case LAST_ACK:
                            if (acknowledgedPacket.isFin()) {
                                state = ConnectionState.CLOSED;
                            }  else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedQueue but should not be");
                            }
                            break;
                    }
                }
            }
        } else {
            //TODO handle invalid acknowledgement
        }
    }

    private void sendAcknowledgement(){
        send(
                new RTP4Packet(tcpBlock.takeSendSequenceNumber(0), tcpBlock.receiveNext, false, true, false, false, tcpBlock.receiveWindow, new byte[0]),
                dstAddr,
                dstPort
        );
    }

    void send(byte[] data, int dstAddr, short dstPort) throws IOException {
        ipSocket.send(data, dstAddr, dstPort);
    }


    @Override
    public void receive(IPacket packet) {
        if (!isClosed() && packet instanceof HRP4Packet) {
            HRP4Packet hrp4Packet = ((HRP4Packet) packet);
            RemoteHost remoteHost = new RemoteHost(hrp4Packet.getSrcAddr(), hrp4Packet.getSrcPort());
            if (remoteHostRTP4ConnectionMap.containsKey(remoteHost)) {
                remoteHostRTP4ConnectionMap.get(remoteHost).receive(packet);
            } else {
                try {
                    RTP4Packet rtp4Packet = new RTP4Packet(hrp4Packet.getData());
                    if (rtp4Packet.isSyn()) {
                        receivedSynQueue.add(hrp4Packet);
                    }
                } catch (PacketMalformedException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    public boolean isClosed() {
        return state == ConnectionState.CLOSED;
    }

    public short getPort(){
        return ipSocket.getDstPort();
    }

}
