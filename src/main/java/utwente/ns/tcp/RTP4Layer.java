package utwente.ns.tcp;

import lombok.Getter;
import utwente.ns.PacketMalformedException;
import utwente.ns.ip.HRP4Packet;
import utwente.ns.ip.IHRP4Layer;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Layer {
    @Getter
    private IHRP4Layer ipLayer;
    private List<RTP4Socket> registeredSockets;
    private List<RTP4Connection> registeredConnections;

    private static final int TIMOUT_MILIS = 5;

    public RTP4Layer(IHRP4Layer ipLayer) {
        this.ipLayer = ipLayer;
        registeredSockets = new ArrayList<>();
        registeredConnections = new ArrayList<>();
        new Thread(this::run,Thread.currentThread().getName() + "-tcpLoop").start();
    }

    private synchronized void run() {
        while (true) {
            System.out.println(Thread.currentThread().getName() + "> Scanning");
            registeredSockets.stream().filter(socket -> socket.state == SocketState.TIME_WAIT).forEach(RTP4Socket::clear);
            registeredSockets = registeredSockets.stream().filter((socket1) -> !socket1.isClosed()).collect(Collectors.toList());
            registeredSockets.forEach(this::handleQueues);
            registeredSockets.forEach(this::resendIfTimeout);
            registeredConnections.forEach(this::sendData);
            registeredSockets.forEach(socket -> System.out.println(Thread.currentThread().getName() + "> " + socket.getPort() + "-" + socket.state));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendData(RTP4Connection connection) {
        if (connection.canSend()) {
            byte[] data = connection.sendDataQueue.poll();
            if (data != null) {
                connection.getSocket().send(data, connection.getAddress(), (short) connection.getPort());
            }
        }
    }

    private void handleQueues(RTP4Socket socket)    {
        SocketAction action = socket.actionQueue.poll();
        if (action != null) {
            System.out.println(Thread.currentThread().getName() + "> Found Action " + action);
            socket.handle(action);
        } else {
            System.out.println(Thread.currentThread().getName() + "> No actions in queue ");
        }
        HRP4Packet packet = socket.receivedPacketQueue.poll();
        if (packet != null) {
            try {
                System.out.println(Thread.currentThread().getName() + "> Found Packet! " + new RTP4Packet(packet.getData()));
            } catch (PacketMalformedException e) {
                System.out.println(Thread.currentThread().getName() + "> Found Packet! ");
            }
            socket.handle(packet);
        } else {
            System.out.println(Thread.currentThread().getName() + "> No Packets in queue ");
        }

    }

    private void resendIfTimeout(RTP4Socket socket) {
        long time = System.currentTimeMillis();
        AbstractMap.Entry<RTP4Packet, Long> entry;
        while (true) {
            entry = socket.unacknowledgedQueue.peek();
            if (entry == null || time - entry.getValue() < TIMOUT_MILIS) {
                break;
            }
            System.out.println(Thread.currentThread().getName() + "> Found Timed out packet! " + entry.getKey());
            socket.unacknowledgedQueue.remove();
            socket.send(entry.getKey(), socket.getDstAddr(), socket.getDstPort());
        }
    }

    void registerConnection(RTP4Connection connection) {
        registeredConnections.add(connection);
    }


    public RTP4Socket open(int port) throws IOException {
        RTP4Socket socket = new RTP4Socket(ipLayer.open((short) port), this);
        registeredSockets.add(socket);
        return socket;
    }

    public RTP4Connection connect(String address, int port) throws IOException {
        RTP4Socket socket = new RTP4Socket(ipLayer.openRandom(), this);
        registeredSockets.add(socket);
        RTP4Connection connection = socket.connect(address, port);
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
    enum SocketAction {
        OPEN, ACCEPT, CLOSE, CONNECT
    }


}
