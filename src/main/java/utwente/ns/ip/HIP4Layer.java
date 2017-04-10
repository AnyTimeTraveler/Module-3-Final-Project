package utwente.ns.ip;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.linklayer.LinkLayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by simon on 07.04.17.
 */
public class HIP4Layer implements IReceiveListener {

    /**
     *
     */
    private final LinkLayer lowerLayer;
    
    /**
     *
     */
    private List<Node> knownNodes;
    
    /**
     *
     */
    private List<Edge> knownEdges;
    
    /**
     * @param linkLayer
     */
    public HIP4Layer(LinkLayer linkLayer) {
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
            List<BCN4Packet.RoutingEntry> routingEntries = new ArrayList<>();
            routingEntries.add(new BCN4Packet.RoutingEntry((byte) 0, Config.getInstance().getBaconPacketTTL(), Util.addressStringToInt(Config.getInstance().getMyAddress()), 0));
            BCN4Packet beaconPacket = new BCN4Packet(routingEntries);
            send(beaconPacket);
        } catch (IOException e) {
            System.err.println("Sending a beacon-packet failed. This shouldn't happen...");
            e.printStackTrace();
        }
    }
    
    public void send(TCP4Packet packet) {
    
    }
    
    public void send(BCN4Packet packet) throws IOException {
        lowerLayer.send(new HIP4Packet(Util.addressToInt(lowerLayer.getLocalAddress()), (short) 0, (short) 0, (byte) 0, (byte) 4, packet.marshal()));
    }
    
    public void addReceiveListener(IReceiveListener receiver) {
    
    }
    
    // For Dijkstra
    private class Node {
        private String address;
    }
    
    private class Edge {
        private String address;
    }
}
