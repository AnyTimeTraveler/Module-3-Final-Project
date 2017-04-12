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
    
    private static String getIdent(byte[] data) {
        if (data.length < 4)
            return "";
        return new StringBuilder().append((char) data[0]).append((char) data[1]).append((char) data[2]).append((char) data[3]).toString();
    }
    
    /**
     *
     */
    private void sendBeaconPacket() {
        try {
            List<BCN4Packet.RoutingEntry> routingEntries = this.router.getRoutingEntries();
            HRP4Packet packet = new HRP4Packet(Util.addressToInt(this.lowerLayer.getLocalAddress()),
                                               0,
                                               (short) 0,
                                               (short) 0,
                                               Config.getInstance().getBaconPacketTTL(),
                                               new byte[0]);
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
        if (!(packet instanceof SimulatedLinkPacket)) {
            return;
        }
        
        byte[] data = packet.getData();
        if (data.length < 4 + HRP4Packet.HEADER_LENGTH)
            return;
        String ident = getIdent(data);
        
        if (!ident.equals("HRP4")) {
            return;
        }
        
        try {
            HRP4Packet hrp4Packet = new HRP4Packet(data);
            
            if (getIdent(hrp4Packet.getData()).equals("BCN4")) {
                int origin = Util.addressToInt(((SimulatedLinkPacket) packet).getReceivedPacketAddress());
                
                Map<Integer, Integer> forwardingTable = this.router.getForwardingTable(origin);
                
                Integer integer = forwardingTable.get(hrp4Packet.getDstAddr());
                integer = integer != null ? integer : -1;
                int i = Util.addressToInt(this.lowerLayer.getLocalAddress());
                if (integer != i) {
                    return;
                }
                
                hrp4Packet.setTTL((byte) (hrp4Packet.getTTL() - 1));
                
                this.send(hrp4Packet);
            } else {
                receiveListeners.forEach(listener -> listener.receive(hrp4Packet));
            }
            
        } catch (PacketMalformedException | IOException ignored) {
        }
    }
}
