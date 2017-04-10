package utwente.ns.tcp;

import utwente.ns.ip.HRP4Layer;

import java.util.List;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Layer {
    private HRP4Layer ipLayer;
    private List<RTP4Socket> registeredSockets;
    private List<RTP4Connection> registeredConnections;

    public RTP4Layer(HRP4Layer ipLayer) {
        this.ipLayer = ipLayer;
    }

    public RTP4Socket open(int port) {
        RTP4Socket socket = new RTP4Socket();
        registeredSockets.add(socket);
        return socket;
    }

    public RTP4Connection connect(String address, int port) {
        RTP4Connection connection = new RTP4Connection();
        registeredConnections.add(connection);
        return connection;
    }
}
