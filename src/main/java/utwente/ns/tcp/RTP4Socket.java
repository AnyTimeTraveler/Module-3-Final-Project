package utwente.ns.tcp;

import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.ip.HRP4Packet;
import utwente.ns.ip.IHRP4Socket;

import java.io.Closeable;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static utwente.ns.tcp.RTP4Layer.SocketState;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Socket implements IReceiveListener, Closeable {
    RTP4Layer rtp4Layer;
    SocketState state;
    private ReentrantLock stateLock = new ReentrantLock();
    private Condition stateChanged = stateLock.newCondition();

    private final IHRP4Socket ipSocket;

    BlockingQueue<HRP4Packet> receivedPacketQueue;
    BlockingQueue<AbstractMap.Entry<RTP4Packet, Long>> unacknowledgedQueue;
    BlockingQueue<RTP4Layer.SocketAction> actionQueue;
    private BlockingQueue<HRP4Packet> receivedSynQueue;

    private List<RTP4Connection> listeningConnections;

    private TCPBlock tcpBlock;

    @Getter
    private int dstAddr;
    @Getter
    private short dstPort;

    private long timeWaitStart;


    RTP4Socket(IHRP4Socket ipSocket, RTP4Layer rtp4Layer) {
        this.ipSocket = ipSocket;
        this.rtp4Layer = rtp4Layer;
        ipSocket.addReceiveListener(this);
        state = SocketState.LISTEN;
        tcpBlock = new TCPBlock();
        receivedPacketQueue = new LinkedBlockingQueue<>();
        receivedSynQueue = new LinkedBlockingQueue<>(10);
        unacknowledgedQueue = new LinkedBlockingQueue<>();
        actionQueue = new LinkedBlockingQueue<>();
        listeningConnections = new LinkedList<>();
    }

    public RTP4Connection accept() throws IOException {
        actionQueue.offer(RTP4Layer.SocketAction.ACCEPT);
        try {
            stateLock.lock();
            while (state != SocketState.ESTABLISHED && state != SocketState.CLOSED) {
                stateChanged.await();
            }
            if (state == SocketState.CLOSED) {
                throw new IOException("Socket got closed");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            stateLock.unlock();
        }
        RTP4Connection rtp4Connection = new RTP4Connection(dstAddr, ((int) dstPort), this);
        addConnection(rtp4Connection);
        rtp4Layer.registerConnection(rtp4Connection);
        return rtp4Connection;
    }

    public RTP4Connection connect(String address, int port) throws IOException {
        dstAddr = Util.addressStringToInt(address);
        dstPort = (short) port;
        actionQueue.offer(RTP4Layer.SocketAction.CONNECT);
        try {
            stateLock.lock();
            while (state != SocketState.ESTABLISHED && state != SocketState.CLOSED) {
                stateChanged.await();
            }
            if (state == SocketState.CLOSED) {
                throw new IOException("Socket got closed");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            stateLock.unlock();
        }
        RTP4Connection rtp4Connection = new RTP4Connection(dstAddr, dstPort, this);
        addConnection(rtp4Connection);
        return rtp4Connection;
    }

    void addConnection(RTP4Connection connection){
        listeningConnections.add(connection);
    }

    void removeConnection(RTP4Connection connection){
        listeningConnections.remove(connection);
    }

    @Override
    public void close() throws IOException {
        actionQueue.offer(RTP4Layer.SocketAction.CLOSE);
        try {
            stateLock.lock();
            while (state != SocketState.CLOSED && state != SocketState.TIME_WAIT) {
                stateChanged.await();
            }
            if (state == SocketState.CLOSED) {
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
                if (System.currentTimeMillis() - timeWaitStart > Config.getInstance().maxSegmentLife) {
                    try {
                        stateLock.lock();
                        state = SocketState.CLOSED;
                    } finally {
                        stateLock.unlock();
                    }
                }
        }
    }

    void handle(HRP4Packet ipPacket) {
        if (state == SocketState.CLOSED || state == SocketState.TIME_WAIT) {
            return;
        }
        //Create RTP4 packet from the data, if the dat is malformed, return
        RTP4Packet packet;
        try {
            packet = new RTP4Packet(ipPacket.getData());
        } catch (PacketMalformedException e) {
            return;
        }

        if (tcpBlock.receiveInitialSeqNumIsSet) {
            if (packet.getSeqNum() < tcpBlock.receiveNext ) {
                if (packet.getLength() > 0) {
                    sendAcknowledgement();
                }
                return;
            } else if (packet.getSeqNum() > tcpBlock.receiveNext) {
                receivedPacketQueue.add(ipPacket);
                return;
            } else {
                tcpBlock.registerReceivedSequenceNumber(packet.getSeqNum(), packet.getLength());
            }
        }

        stateLock.lock();
        try {
            switch (state) {
                case LISTEN:
                    if (packet.isSyn()) {
                        receivedSynQueue.offer(ipPacket);
                    }
                    break;
                case SYN_ACCEPTED:
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
                case SYN_SENT:
                    if (packet.isSyn()) {
                        tcpBlock.registerReceivedSequenceNumber(packet.getSeqNum(), packet.getLength());
                        sendAcknowledgement();
                        state = SocketState.SYN_ACCEPTED;
                    }
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
                case ESTABLISHED:
                    if (packet.isFin()) {
                        sendAcknowledgement();
                        state = SocketState.CLOSE_WAIT;
                    } else {
                        if (packet.getData().length > 0) {
                            listeningConnections.forEach(connection -> connection.receiveData(packet.getData()));
                        }
                        if (packet.getLength() > 0){
                            sendAcknowledgement();
                        }
                    }
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
                case FIN_WAIT_1:
                    if (packet.isFin()) {
                        state = SocketState.CLOSING;
                        sendAcknowledgement();
                    }
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
                case FIN_WAIT_2:
                    if (packet.isFin()) {
                        sendAcknowledgement();
                        state = SocketState.TIME_WAIT;
                        timeWaitStart = System.currentTimeMillis();
                    }
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
                case CLOSING:
                case CLOSE_WAIT:
                case LAST_ACK:
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
            }
            stateChanged.signal();
        } finally {
            stateLock.unlock();
        }
    }

    void handle(RTP4Layer.SocketAction action){
        switch (action) {
            case ACCEPT:
                HRP4Packet packet;
                try {
                    packet = receivedSynQueue.peek();
                    if (packet != null) {
                        receivedSynQueue.remove();
                        dstAddr = packet.getSrcAddr();
                        dstPort = packet.getSrcPort();
                        RTP4Packet rtp4Packet = new RTP4Packet(packet.getData());
                        tcpBlock.registerReceivedSequenceNumber(rtp4Packet.getSeqNum(),rtp4Packet.getLength());
                        sendControl(true, true, false);
                        try {
                            stateLock.lock();
                            state = SocketState.SYN_ACCEPTED;
                            stateChanged.signal();
                        } finally {
                            stateLock.unlock();
                        }
                    } else {
                        actionQueue.offer(action);
                    }
                } catch (PacketMalformedException e) {
                    e.printStackTrace();
                }
                break;
            case CLOSE:
                switch (state) {
                    case LISTEN:
                    case SYN_SENT:
                        actionQueue.offer(action);
                        break;
                    case SYN_ACCEPTED:
                    case ESTABLISHED:
                        try {
                            stateLock.lock();
                            state = SocketState.FIN_WAIT_1;
                            stateChanged.signal();
                        } finally {
                            stateLock.unlock();
                        }
                        sendControl(false,false,true);
                        break;
                    case CLOSE_WAIT:
                        try {
                            stateLock.lock();
                            state = SocketState.LAST_ACK;
                            stateChanged.signal();
                        } finally {
                            stateLock.unlock();
                        }
                        sendControl(false,false,true);
                }
                break;
            case CONNECT:
                sendControl(true, false, false);
                try {
                    stateLock.lock();
                    state = SocketState.SYN_SENT;
                    stateChanged.signal();
                } finally {
                    stateLock.unlock();
                }
                break;
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
                                state = SocketState.ESTABLISHED;
                            }
                            break;
                        case FIN_WAIT_1:
                            if (acknowledgedPacket.isFin()) {
                                state = SocketState.FIN_WAIT_2;
                            }
                            break;
                        case CLOSING:
                            if (acknowledgedPacket.isFin()) {
                                state = SocketState.TIME_WAIT;
                                timeWaitStart = System.currentTimeMillis();
                            }
                            break;
                        case LAST_ACK:
                            if (acknowledgedPacket.isFin()) {
                                state = SocketState.CLOSED;
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

    void send(byte[] data, int dstAddr, short dstPort) {
        send(
                new RTP4Packet(tcpBlock.takeSendSequenceNumber(data.length), tcpBlock.receiveNext, false, false, false, false, (short) 0, data),
                dstAddr,
                dstPort
        );
    }

    void send(RTP4Packet packet, int dstAddr, short dstPort) {
        try {
            ipSocket.send(packet.marshal(), dstAddr, dstPort);
            if (packet.getLength() > 0) {
                unacknowledgedQueue.add(new AbstractMap.SimpleEntry<>(packet, System.currentTimeMillis()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    private void sendControl(boolean syn, boolean ack, boolean fin) {
        send(
                new RTP4Packet(tcpBlock.takeSendSequenceNumber(syn || fin ? 1 : 0), tcpBlock.receiveNext, syn, ack, fin, false, tcpBlock.receiveWindow, new byte[0]),
                dstAddr,
                dstPort
        );
    }

    @Override
    public void receive(IPacket packet) {
        if (state != SocketState.CLOSED) {
            receivedPacketQueue.offer((HRP4Packet) packet);
        }
    }

    public boolean isClosed() {
        return state == SocketState.CLOSED;
    }

    public int getPort(){
        return ipSocket.getDstPort();
    }

    class TCPBlock {

        private int sendUnacknowledged;
        private int sendNext;
        private short sendWindow;
        private int sendWindowUpdateSeqNum;
        private int sendWindowUpdateAckNum;
        private final int sendInitialSeqNum;

        private int receiveNext;
        private boolean receiveInitialSeqNumIsSet;
        private short receiveWindow;
        private int receiveInitialSeqNum;

        TCPBlock() {
            //TODO reset to non debug init
//            this.sendInitialSeqNum = (int) (System.nanoTime() / 4000);
            this.sendInitialSeqNum = new Random().nextInt(100);
            this.sendNext = this.sendInitialSeqNum;
            this.sendUnacknowledged = this.sendInitialSeqNum - 1;
            this.receiveInitialSeqNumIsSet = false;
        }

        int takeSendSequenceNumber(int length) {
            int sequenceNumberCurrent = this.sendNext;
            this.sendNext += length;
            return sequenceNumberCurrent;
        }

        boolean registerReceivedSequenceNumber(int sequenceNumber, int length) {
            if (receiveNext == sequenceNumber) {
                receiveNext += length;
                return true;
            }  else if (!receiveInitialSeqNumIsSet){
                receiveInitialSeqNum = sequenceNumber;
                receiveInitialSeqNumIsSet = true;
                receiveNext = receiveInitialSeqNum + length;
                return true;
            } if (sequenceNumber > receiveNext) {
                return false;
            } else {
                return false;
            }
        }

        public boolean registerReceivedAcknowledgeNumber(int acknowledgeNumber) {
            if (acknowledgeNumber > sendUnacknowledged && acknowledgeNumber <= sendNext) {
                sendUnacknowledged = acknowledgeNumber;
                return true;
            } else {
                return false;
            }
        }

    }

}
