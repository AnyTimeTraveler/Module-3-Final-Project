package utwente.ns.tcp;

import lombok.Getter;
import utwente.ns.ip.IHRP4Layer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Layer {
    @Getter
    private IHRP4Layer ipLayer;
    private List<RTP4Socket> registeredSockets;
    private List<RTP4Connection> registeredConnections;


    public RTP4Layer(IHRP4Layer ipLayer) {
        this.ipLayer = ipLayer;
        registeredSockets = new ArrayList<>();
        registeredConnections = new ArrayList<>();
        new Thread(this::run,Thread.currentThread().getName() + "-tcpLoop").start();
    }

    private synchronized void run() {
        while (true) {
            System.out.println(Thread.currentThread().getName() + "> Scanning");
            registeredConnections.stream().filter(connection -> connection.getState() == ConnectionState.TIME_WAIT).forEach(RTP4Connection::clear);
            registeredConnections = registeredConnections.stream()
                    .filter(connection -> connection.getState() != ConnectionState.CLOSED || connection.getState() != ConnectionState.TIME_WAIT)
                    .collect(Collectors.toList());
            registeredConnections.forEach(RTP4Connection::handlePacket);
            registeredConnections.forEach(RTP4Connection::handleAction);
            registeredConnections.forEach(RTP4Connection::resendPacket);
            registeredConnections.forEach(connection -> System.out.println(Thread.currentThread().getName() + "> " + connection.getRemoteHost().getPort() + "-" + connection.getState()));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void registerConnection(RTP4Connection connection) {
        registeredConnections.add(connection);
    }

    void removeConnection(RTP4Connection rtp4Connection) {
        registeredConnections.remove(rtp4Connection);
    }


    public RTP4Socket
    open(int port) throws IOException {
        RTP4Socket socket = new RTP4Socket(ipLayer.open((short) port), this);
        registeredSockets.add(socket);
        return socket;
    }

    public RTP4Connection connect(String address, int port, long timeout) throws IOException, TimeoutException {
        RTP4Socket socket = new RTP4Socket(ipLayer.openRandom(), this);
        registeredSockets.add(socket);
        RTP4Connection connection = socket.connect(address, port);
        return connection;
    }

    enum ConnectionState {
        SYN_ACCEPTED,
        SYN_SENT,
        ESTABLISHED,
        FIN_WAIT_1,
        FIN_WAIT_2,
        CLOSING,
        TIME_WAIT,
        CLOSE_WAIT,
        LAST_ACK,
        CLOSED,
    }
    enum ConnectionAction {
        CLOSE, CONNECT, SEND
    }


}
