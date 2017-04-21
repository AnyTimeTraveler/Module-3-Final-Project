package utwente.ns.linklayer;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.config.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Created by simon on 07.04.17.
 */
public class VirtualLinkLayer implements ILinkLayer {

    private List<IReceiveListener> packetListeners;
    private InetAddress[] addresses;
    private MulticastSocket socket;
    private Thread receiver;
    private boolean closed;
    private IntSupplier maxSegmentSize;

    private VirtualLinkLayer() throws IOException {
        packetListeners = new ArrayList<>();
        socket = new MulticastSocket(Config.getInstance().multicastPort);
        receiver = new Thread(this::waitForIncomingPackets);
        receiver.setDaemon(true);
        receiver.setName("LinkLayerReceiver");
        receiver.start();
    }

    public VirtualLinkLayer(IntSupplier maxSegmentSize) throws IOException {
        this();
        this.maxSegmentSize = maxSegmentSize;
        addresses = new InetAddress[1];
        addresses[0] = InetAddress.getByName(Config.getInstance().multicastAddress);
        socket.joinGroup(addresses[0]);

    }

    public VirtualLinkLayer(IntSupplier maxSegmentSize, InetAddress... multicastAddresses) throws IOException {
        this();
        this.maxSegmentSize = maxSegmentSize;
        this.addresses = multicastAddresses;
        for (InetAddress address : addresses) {
            socket.joinGroup(address);
        }
    }

    @Override
    public void send(IPacket packet) throws IOException {
        send(packet.marshal());
    }

    @Override
    public void send(byte[] data) throws IOException {
        for (InetAddress address : addresses) {
            socket.send(new DatagramPacket(data, data.length, address, Config.getInstance().multicastPort));
        }
    }
    @Override
    public void addReceiveListener(IReceiveListener receiver) {
        packetListeners.add(receiver);
    }

    @Override
    public void removeReceiveListener(IReceiveListener receiver) {
        packetListeners.remove(receiver);
    }

    private void waitForIncomingPackets() {
        closed = false;
        while (!closed) {
            byte[] receivedData = new byte[maxSegmentSize.getAsInt()];
            DatagramPacket receivedPacket = new DatagramPacket(receivedData, maxSegmentSize.getAsInt());
            try {
                socket.receive(receivedPacket);
                for (IReceiveListener listener : packetListeners) {
                    listener.receive(new VirtualLinkPacket(receivedPacket));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
