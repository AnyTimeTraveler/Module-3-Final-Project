package utwente.ns.ip;

import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.linklayer.ILinkLayer;
import utwente.ns.linklayer.SimulatedLinkPacket;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class HRP4Layer implements IReceiveListener {

    /**
     *
     */
    @Getter
    private final ILinkLayer lowerLayer;

    private HRP4Router router = new HRP4Router(this);

    private List<IReceiveListener> receiveListeners;
    
    /**
     * @param linkLayer
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
        }, (long) Config.getInstance().getBaconInterval(), (long) Config.getInstance().getBaconInterval());
    }

    /**
     *
     */
    private void sendBeaconPacket() {
        try {
            List<HRP4Router.BCNRoutingEntryAlternative> routingEntries = this.router.getRoutingEntries();
            HRP4Packet packet = new HRP4Packet(
                    Util.addressToInt(InetAddress.getByName(Config.getInstance().getMyAddress())),
                    0,
                    (short) 0,
                    (short) 0,
                    (byte) 0,
                    new byte[0]
            );
            BCN4Packet beaconPacket = new BCN4Packet(routingEntries, packet);
            packet.setData(beaconPacket.marshal());
            send(packet);
        } catch (IOException e) {
            System.err.println("Sending a beacon-packet failed. This shouldn't happen...");
            e.printStackTrace();
        }
    }


    public void send(IPacket packet) throws IOException {
        lowerLayer.send(packet);
    }
    
    public void addReceiveListener(IReceiveListener receiver) {
        receiveListeners.add(receiver);
    }

    public HRP4Socket open(short port) throws IOException {
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

    void close(HRP4Socket socket) {
        this.receiveListeners.remove(socket);
    }

    @Override
    public void receive(IPacket packet) {
        try {
            HRP4Packet hrp4Packet = new HRP4Packet(packet.getData());
            int myAddr = Util.addressToInt(InetAddress.getByName(Config.getInstance().getMyAddress()));

            if (hrp4Packet.getDstAddr() == Util.addressToInt(InetAddress.getByName(Config.getInstance().getMyAddress())) ||
                    hrp4Packet.getDstAddr() == 0) {
                try {
                    BCN4Packet p = new BCN4Packet(hrp4Packet, hrp4Packet.getData());
                    router.update(p);
                } catch (PacketMalformedException e) {
                    receiveListeners.forEach(listener -> listener.receive(hrp4Packet));
                }
            }

            if (hrp4Packet.getTTL() >= 1 && hrp4Packet.getDstAddr() != myAddr) {
                int origin = Util.addressToInt(((SimulatedLinkPacket) packet).getReceivedPacketAddress());

                Map<Integer, Integer> forwardingTable = this.router.getForwardingTable(origin);

                if ((
                        forwardingTable.get(hrp4Packet.getDstAddr()) != null &&
                        forwardingTable.get(hrp4Packet.getDstAddr()) == myAddr) ||
                      hrp4Packet.getDstAddr() == 0) {

                    hrp4Packet.setTTL((byte) (hrp4Packet.getTTL() - 1));

                    this.send(hrp4Packet);
                }
            }
        } catch (PacketMalformedException | IOException ignored) { }
    }
}
