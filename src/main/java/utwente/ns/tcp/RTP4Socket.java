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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Socket implements IReceiveListener, Closeable {
    private RTP4Layer rtp4Layer;

    private Map<RemoteHost, RTP4Connection> remoteHostRTP4ConnectionMap;

    private final IHRP4Socket ipSocket;

    private BlockingQueue<HRP4Packet> receivedSynQueue;

    RTP4Socket(IHRP4Socket ipSocket, RTP4Layer rtp4Layer) {
        this.ipSocket = ipSocket;
        this.rtp4Layer = rtp4Layer;
        ipSocket.addReceiveListener(this);
        receivedSynQueue = new LinkedBlockingQueue<>(10);
        remoteHostRTP4ConnectionMap = new HashMap<>();
    }

    RTP4Connection accept() throws IOException, TimeoutException {
        RTP4Connection rtp4Connection;
        RemoteHost remoteHost;
        while (true) {
            HRP4Packet synPacket;
            long timeStart = System.currentTimeMillis();
            long timeout = Config.getInstance().getTcpListenTimeout();
            while (true) {
                synPacket = receivedSynQueue.poll();
                if (synPacket != null) {
                    break;
                }
                if (timeout >= 0 && System.currentTimeMillis() - timeStart > timeout) {
                    throw new TimeoutException("Got no connection request within timeout");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            remoteHost = new RemoteHost(synPacket.getSrcAddr(), synPacket.getSrcPort());
            rtp4Connection = new RTP4Connection(remoteHost, this);
            addConnection(remoteHost, rtp4Connection);
            rtp4Layer.registerConnection(rtp4Connection);
            try {
                rtp4Connection.accept(synPacket);
                break;
            } catch (PacketMalformedException e) {
                removeConnection(remoteHost);
                rtp4Layer.removeConnection(rtp4Connection);
            }
        }
        return rtp4Connection;
    }

    RTP4Connection connect(String address, int port) throws IOException, TimeoutException {
        RemoteHost remoteHost = new RemoteHost(Util.addressStringToInt(address), (short) port);
        RTP4Connection rtp4Connection = new RTP4Connection(remoteHost, this);
        addConnection(remoteHost, rtp4Connection);
        rtp4Layer.registerConnection(rtp4Connection);
        rtp4Connection.connect();
        return rtp4Connection;
    }

    private void addConnection(RemoteHost remoteHost, RTP4Connection connection){
        remoteHostRTP4ConnectionMap.put(remoteHost, connection);
    }

    void removeConnection(RemoteHost remoteHost){
        remoteHostRTP4ConnectionMap.remove(remoteHost);
    }


    void send(byte[] data, int dstAddr, short dstPort) throws IOException {
        ipSocket.send(data, dstAddr, dstPort);
    }


    @Override
    public void receive(IPacket packet) {
        if (packet instanceof HRP4Packet) { //TODO Check if not closed
            HRP4Packet hrp4Packet = ((HRP4Packet) packet);
            RemoteHost remoteHost = new RemoteHost(hrp4Packet.getSrcAddr(), hrp4Packet.getSrcPort());
            if (remoteHostRTP4ConnectionMap.containsKey(remoteHost)) {
                remoteHostRTP4ConnectionMap.get(remoteHost).receive(packet);
            } else {
                try {
                    RTP4Packet rtp4Packet = new RTP4Packet(hrp4Packet.getData());
                    if (rtp4Packet.isSyn()) {
                        System.out.println(System.currentTimeMillis() + "> " + "Found SYN: " + rtp4Packet);
                        receivedSynQueue.add(hrp4Packet);
                    }
                } catch (PacketMalformedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public short getPort(){
        return ipSocket.getDstPort();
    }

    @Override
    public void close() throws IOException {
        remoteHostRTP4ConnectionMap.entrySet().forEach(entry -> {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
                //TODO throw exception up
            }
        });
        ipSocket.close();
    }
}
