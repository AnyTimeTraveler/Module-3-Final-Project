package utwente.ns.ip;

import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.linklayer.ILinkLayer;
import utwente.ns.linklayer.VirtualLinkPacket;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

/**
 * HRP4Layer is the management class for HRP4 data connections and routing them.
 */
public class HRP4Layer implements IReceiveListener, IHRP4Layer {

    /**
     * The next layer down (an ILinkLayer)
     */
    @Getter
    private final ILinkLayer lowerLayer;

    /**
     * The router that is responsible for figuring out optimal routes to certain hosts.
     */
    @Getter
    private HRP4Router router = new HRP4Router();

    /**
     * All the listeners that are listening for incoming data.
     */
    private List<IReceiveListener> receiveListeners;

    /**
     * Set of opened ports
     */
    private NavigableSet<Integer> ports = new TreeSet<>();

    /**
     * Constructs a new HRP4Layer that uses the parameter as the sending layer
     *
     * @param linkLayer the layer that is used to send raw data.
     */
    public HRP4Layer(ILinkLayer linkLayer) {
        receiveListeners = new ArrayList<>();
        lowerLayer = linkLayer;
        lowerLayer.addReceiveListener(this);
        Timer beaconTimer = new Timer();
        beaconTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendBeaconPacket();
            }
        }, Config.getInstance().baconInterval, Config.getInstance().baconInterval);
    }

    /**
     * sendBeaconPacket sends a beacon packet containing all routing entries.
     */
    private void sendBeaconPacket() {
        try {
            List<HRP4Router.BCN4RoutingEntryWrapper> routingEntries = this.router.getRoutingEntries();
            HRP4Packet packet = new HRP4Packet(Util.addressToInt(InetAddress.getByName(Config.getInstance().myAddress)), 0, (short) 0, (short) 0, (byte) 0, new byte[0]);
            BCN4Packet beaconPacket = new BCN4Packet(routingEntries, packet);
            packet.setData(beaconPacket.marshal());
            send(packet);
        } catch (IOException e) {
            System.err.println("Sending a beacon-packet failed. This shouldn't happen...");
            e.printStackTrace();
        }
    }

    /**
     * Send sends the specific packet from the lower layer. This is equivalent to calling {@link HRP4Layer#getLowerLayer()#send(IPacket)}
     *
     * @param packet the packet to be successful
     * @throws IOException when sending fails
     */
    public void send(IPacket packet) throws IOException {
        lowerLayer.send(packet);
    }

    /**
     * Adds a receive listener that is called upon receiving an HRP4-compatible packet.
     *
     * @param receiver is called upon receiving a packet.
     */
    public void addReceiveListener(IReceiveListener receiver) {
        receiveListeners.add(receiver);
        if (receiver instanceof IHRP4Socket) {
            ports.add((int) ((IHRP4Socket) receiver).getDstPort());
        }
    }

    /**
     * Opens a socket on the provided port, and registers it on this layer as a listener.
     *
     * @param port the port to be opened
     * @return the socket that resulted by opening the port.
     * @throws IOException when the port is already opened
     */
    public HRP4Socket open(int port) throws IOException {
        if (this.receiveListeners.parallelStream()
                .filter(listener -> listener instanceof HRP4Socket)
                .map(listener -> (HRP4Socket) listener)
                .anyMatch(listener -> listener.getDstPort() == port)) {
            throw new IOException("Port already opened");
        }

        HRP4Socket socket = new HRP4Socket(this, port);
        this.addReceiveListener(socket);
        return socket;
    }

    /**
     * Opens a random port in the range of 1024..65535
     *
     * @return the opend socket
     * @throws IOException when failing to open a socket (usually meaning a failure).
     */
    @Override
    public IHRP4Socket openRandom() throws IOException {
        return this.open(Util.randomNotInSet(ports, 1024, 32767));
    }

    void close(HRP4Socket socket) {
        this.receiveListeners.remove(socket);
        this.ports.remove((int) socket.getDstPort());
    }

    @Override
    public void receive(IPacket packet) {
        try {
            HRP4Packet hrp4Packet = new HRP4Packet(packet.getData());
            int myAddr = Util.addressToInt(InetAddress.getByName(Config.getInstance().myAddress));

            if (hrp4Packet.getDstAddr() == Util.addressToInt(InetAddress.getByName(Config.getInstance().myAddress)) || hrp4Packet.getDstAddr() == 0) {
                try {
                    BCN4Packet p = new BCN4Packet(hrp4Packet, hrp4Packet.getData());
                    router.update(p);
                } catch (PacketMalformedException e) {
                    receiveListeners.forEach(listener -> listener.receive(hrp4Packet));
                }
            }

            if (hrp4Packet.getTTL() >= 1 && hrp4Packet.getDstAddr() != myAddr) {
                int origin = Util.addressToInt(((VirtualLinkPacket) packet).getReceivedPacketAddress());

                Map<Integer, Integer> forwardingTable = this.router.getForwardingTable(origin);

                // TODO: Remove if-statement to do smart routing (0.o)
                 if ((forwardingTable.get(hrp4Packet.getDstAddr()) != null && forwardingTable.get(hrp4Packet.getDstAddr()) == myAddr) || hrp4Packet.getDstAddr() == 0) {

                    hrp4Packet.setTTL((byte) (hrp4Packet.getTTL() - 1));

                    this.send(hrp4Packet);
                 }
            }
        } catch (PacketMalformedException | IOException ignored) {
        }
    }
}
