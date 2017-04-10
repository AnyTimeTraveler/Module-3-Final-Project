package utwente.ns.ip;

import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.linklayer.ILinkLayer;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HRP4Layer implements IReceiveListener {

    /**
     *
     */
    @Getter
    private final ILinkLayer lowerLayer;

    private HRP4Router router = new HRP4Router(this);
    
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
            List<BCN4Packet.RoutingEntry> routingEntries = this.router.getRoutingEntries();
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


    public void send(IPacket packet) throws IOException {
        lowerLayer.send(packet);
    }
    
    public void addReceiveListdener(IReceiveListener receiver) {
    
    }
    
    @Override
    public void receive(IPacket packet) {
        // TODO: Deal with unmarshalling crap
    }
}
