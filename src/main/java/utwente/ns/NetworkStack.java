package utwente.ns;

import lombok.Getter;
import utwente.ns.config.Config;
import utwente.ns.ip.HRP4Layer;
import utwente.ns.linklayer.SimulatedLinkLayer;
import utwente.ns.tcp.RTP4Layer;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by simon on 12.04.17.
 */
public class NetworkStack {
    @Getter
    private SimulatedLinkLayer linkLayer;
    @Getter
    private HRP4Layer hrp4Layer;
    @Getter
    private RTP4Layer rtp4Layer;
    
    public NetworkStack() throws IOException {
        this(Config.getInstance().getSegmentBufferSize());
    }
    
    public NetworkStack(int maxSegmentSize, InetAddress... multicastAddresses) throws IOException {
        linkLayer = new SimulatedLinkLayer(maxSegmentSize, multicastAddresses);
        hrp4Layer = new HRP4Layer(linkLayer);
        rtp4Layer = new RTP4Layer(hrp4Layer);
    }
    
    public NetworkStack(int maxSegmentSize) throws IOException {
        linkLayer = new SimulatedLinkLayer(maxSegmentSize);
        hrp4Layer = new HRP4Layer(linkLayer);
        rtp4Layer = new RTP4Layer(hrp4Layer);
    }
}
