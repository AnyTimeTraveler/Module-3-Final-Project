package utwente.ns.tcp;

import lombok.Getter;
import utwente.ns.Util;
import utwente.ns.ip.HRP4Layer;
import utwente.ns.ip.HRP4Packet;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Layer {
    @Getter
    private HRP4Layer ipLayer;
    private List<RTP4Socket> registeredSockets;
    private List<RTP4Connection> registeredConnections;

    public RTP4Layer(HRP4Layer ipLayer) {
        this.ipLayer = ipLayer;
        registeredSockets = new ArrayList<>();
        registeredConnections = new ArrayList<>();
        new Thread(this::run).start();
    }

    private synchronized void run() {
        while (true) {
            registeredSockets.forEach(this::handleIfPacketInQueueOfSocket);
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void handleIfPacketInQueueOfSocket(RTP4Socket socket) {
        HRP4Packet packet = socket.receivedPacketQueue.poll();
        if (packet != null) {
            socket.handle(packet);
        }
    }

    public RTP4Socket open(int port) throws IOException {
        RTP4Socket socket = new RTP4Socket(ipLayer.open((short) port));
        registeredSockets.add(socket);
        return socket;
    }

    public RTP4Connection connect(String address, int port) throws UnknownHostException {
        RTP4Socket socket = null;
        while (socket == null) {
            try {
                socket = new RTP4Socket(ipLayer.open((short) (new Random().nextInt(1000) + 27000)));
            } catch (IOException ignored) {
            }
        }
        RTP4Connection connection = new RTP4Connection(Util.addressStringToInt(address), port, socket);
        registeredConnections.add(connection);
        return connection;
    }

    enum SocketState {
        CLOSED,
        LISTEN,
        SYN_ACCEPTED,
        SYN_SENT,
        ESTABLISHED,
        FIN_WAIT_1,
        FIN_WAIT_2,
        CLOSING,
        TIME_WAIT,
        CLOSE_WAIT,
        LAST_ACK
    }


}
