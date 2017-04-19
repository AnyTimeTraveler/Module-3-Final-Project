package utwente.ns.tcp;

import lombok.Getter;
import utwente.ns.config.Config;
import utwente.ns.ip.IHRP4Layer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Layer {
    private static final int SEND_INTERVAL = Config.getInstance().tcpPacketInterval;
    public static final boolean DEBUG = true;
    //TODO remove

    @Getter
    private IHRP4Layer ipLayer;
    private List<RTP4Socket> registeredSockets;
    private final List<RTP4Connection> registeredConnections;
    private BlockingQueue<AbstractMap.Entry<Boolean, RTP4Connection>> connectionRegisterRequests;

    public RTP4Layer(IHRP4Layer ipLayer) {
        this.ipLayer = ipLayer;
        registeredSockets = new ArrayList<>();
        registeredConnections = new ArrayList<>();
        connectionRegisterRequests = new LinkedBlockingQueue<>();
        new Thread(this::run, Thread.currentThread().getName() + "-tcpLoop").start();
    }

    private void run() {
        while (true) {
            synchronized (registeredConnections) {
                //if (DEBUG) System.out.println(Thread.currentThread().getName() + "> Scanning");
                registeredConnections.stream().filter(connection -> connection.getState() == ConnectionState.TIME_WAIT).forEach(RTP4Connection::clear);
                registeredConnections.removeAll(registeredConnections.stream()
                        .filter(connection -> connection.getState() == ConnectionState.CLOSED)
                        .collect(Collectors.toList()));
                connectionRegisterRequests.forEach(entry -> {
                    if (entry.getKey()) {
                        registeredConnections.add(entry.getValue());
                    } else {
                        registeredConnections.remove(entry.getValue());
                    }
                });
                connectionRegisterRequests.clear();
                registeredConnections.forEach(RTP4Connection::handlePacket);
                registeredConnections.forEach(RTP4Connection::handleAction);
                registeredConnections.forEach(RTP4Connection::resendPacket);
                if (DEBUG)
                    log();
            }
            try {
                Thread.sleep(SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    synchronized void registerConnection(RTP4Connection rtp4Connection) {
        connectionRegisterRequests.add(new AbstractMap.SimpleEntry<>(true, rtp4Connection));
    }

    synchronized void removeConnection(RTP4Connection rtp4Connection) {
        connectionRegisterRequests.add(new AbstractMap.SimpleEntry<>(false, rtp4Connection));
    }


    public RTP4Socket
    open(int port) throws IOException {
        RTP4Socket socket = new RTP4Socket(ipLayer.open(port), this);
        registeredSockets.add(socket);
        return socket;
    }

    public RTP4Connection connect(String address, int port) throws IOException, TimeoutException {
        RTP4Socket socket = new RTP4Socket(ipLayer.openRandom(), this);
        registeredSockets.add(socket);
        return socket.connect(address, port);
    }

    enum ConnectionState {
        SYN_ACCEPTED,
        SYN_SENT_1,
        SYN_SENT_2,
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

    private Map<RTP4Connection, String> conLogTuples = new HashMap<>();
    private void log() {
        for (RTP4Connection registeredConnection : registeredConnections) {
            String logVal = Thread.currentThread().getName() + "> " + registeredConnection.getRemoteHost().getPort() + "-" + registeredConnection.getState();
            if (!conLogTuples.containsKey(registeredConnection) || !conLogTuples.get(registeredConnection).equals(logVal)) {
                System.out.println(logVal);
                conLogTuples.put(registeredConnection, logVal);
            }
        }
    }
}
