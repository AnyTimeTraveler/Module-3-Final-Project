package utwente.ns.linklayer;

import utwente.ns.IReceiveListener;
import utwente.ns.ip.HIP4Packet;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by simon on 07.04.17.
 */
public class LinkLayer implements Closeable {

    private static Properties properties;

    private List<IReceiveListener> packetListeners;
    private InetAddress address;
    private MulticastSocket socket;
    private Thread receiver;
    private boolean closed;
    private int maxSegmentSize;

    public LinkLayer(int maxSegmentSize) throws IOException {
        properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("network.properties"));
        this.maxSegmentSize = maxSegmentSize;
        packetListeners = new ArrayList<>();
        address = InetAddress.getByName(properties.getProperty("linkstate.address", "228.0.0.1"));
        socket = new MulticastSocket(Integer.parseInt(properties.getProperty("linkstate.port", "1337")));
        receiver = new Thread(this::waitForIncomingPackets);
        receiver.setDaemon(true);
        receiver.setName("LinkLayerReceiver");
        receiver.start();
    }

    public void send(HIP4Packet packet) throws IOException {
        byte[] data = packet.marshal();
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, Integer.parseInt(properties.getProperty("linkstate.port", "1337")));
        socket.send(sendPacket);
    }

    public void send(byte[] data) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, Integer.parseInt(properties.getProperty("linkstate.port", "1337")));
        socket.send(sendPacket);
    }

    public void addReceiveListener(IReceiveListener receiver) {
        packetListeners.add(receiver);
    }

    public void removeReceiveListener(IReceiveListener receiver) {
        packetListeners.remove(receiver);
    }

    private void waitForIncomingPackets() {
        closed = false;
        while (!closed) {
            byte[] receivedData = new byte[maxSegmentSize];
            DatagramPacket receivedPacket = new DatagramPacket(receivedData, maxSegmentSize);
            try {
                socket.receive(receivedPacket);
                for (IReceiveListener listener : packetListeners) {
                    listener.receive(new LinkPacket(receivedPacket));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getLinkCost(String address) {
        return 1;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
