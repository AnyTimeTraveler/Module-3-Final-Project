package utwente.ns.ip;

import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.linklayer.ILinkLayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HRP4Layer implements IReceiveListener {

    /**
     *
     */
    @Getter
    private final ILinkLayer lowerLayer;
    
    /**
     * @param linkLayer
     */
    public HRP4Layer(ILinkLayer linkLayer) {
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
            HRP4Packet packet = new HRP4Packet(
                    Util.addressToInt(this.lowerLayer.getLocalAddress()),
                    0,
                    (short) 0,
                    (short) 0,
                    Config.getInstance().getBaconPacketTTL(),
                    new byte[0]
            );
            BCN4Packet beaconPacket = new BCN4Packet(packet, routingEntries);
            packet.setData(beaconPacket.marshal());
            send(packet);
        } catch (IOException e) {
            System.err.println("Sending a beacon-packet failed. This shouldn't happen...");
            e.printStackTrace();
        }
    }


    public void send(HRP4Packet packet) throws IOException {
        lowerLayer.send(packet);
    }
    
    public void addReceiveListener(IReceiveListener receiver) {
    
    }
    
    @Override
    public void receive(IPacket packet) {
        // TODO: Deal with unmarshalling crap
    }
}
