package utwente.ns.tcp;

import utwente.ns.ip.HIP4Layer;

import java.util.List;

/**
 * Created by simon on 07.04.17.
 */
public class TCP4Layer {
    private HIP4Layer ipLayer;
    private List<TCP4Socket> registeredSockets;
    private List<TCP4Connection> registeredConnections;

    public TCP4Layer(HIP4Layer ipLayer) {
        this.ipLayer = ipLayer;
    }

    public TCP4Socket open(int port) {
        TCP4Socket socket = new TCP4Socket();
        registeredSockets.add(socket);
        return socket;
    }

    public TCP4Connection connect(String address, int port) {
        TCP4Connection connection = new TCP4Connection();
        registeredConnections.add(connection);
        return connection;
    }
}
