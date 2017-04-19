package utwente.ns.tcp;

import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.config.Config;
import utwente.ns.ip.HRP4Packet;

import java.io.Closeable;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Connection implements Closeable, IReceiveListener {
    private static final long PACKET_TIMEOUT_MILLIS = Config.getInstance().tcpPacketTimeout;
    private static final long LISTEN_TIMEOUT_MILLIS = Config.getInstance().tcpListenTimeout;
    @Getter
    private RemoteHost remoteHost;
    @Getter
    private final RTP4Socket socket;

    BlockingQueue<AbstractMap.Entry<RTP4Packet, Long>> unacknowledgedPacketQueue;
    BlockingQueue<byte[]> unAcknowledgedDataQueue;
    private BlockingQueue<byte[]> receivedDataQueue;
    BlockingQueue<byte[]> sendDataQueue;


    private RTP4Layer.ConnectionState state;

    RTP4Layer.ConnectionState getState() {
        return state;
    }


    private BlockingQueue<HRP4Packet> receivedPacketQueue;
    private BlockingQueue<RTP4Layer.ConnectionAction> actionQueue;
    private TCPBlock tcpBlock;
    private long timeWaitStart;

    RTP4Connection(RemoteHost remoteHost, RTP4Socket socket) {
        this.remoteHost = remoteHost;
        this.socket = socket;
        this.receivedDataQueue = new LinkedBlockingQueue<>();
        this.sendDataQueue = new LinkedBlockingQueue<>();
        this.actionQueue = new LinkedBlockingQueue<>();
        this.receivedPacketQueue = new LinkedBlockingQueue<>();
        this.unacknowledgedPacketQueue = new LinkedBlockingQueue<>();
        this.unAcknowledgedDataQueue = new LinkedBlockingQueue<>();
        this.tcpBlock = new TCPBlock();
        this.state = RTP4Layer.ConnectionState.CLOSED;
    }

    RTP4Connection(int address, int port, RTP4Socket socket) {
        this(new RemoteHost(address, port), socket);
    }

    void connect() throws TimeoutException {
        actionQueue.offer(RTP4Layer.ConnectionAction.CONNECT);
        waitForEstablishment(LISTEN_TIMEOUT_MILLIS);
    }

    void accept(HRP4Packet synPacket) throws PacketMalformedException, TimeoutException {
        RTP4Packet rtp4Packet = new RTP4Packet(synPacket.getData());
        if (RTP4Layer.DEBUG) System.out.println(Thread.currentThread().getName() + "> Found SYN! " + rtp4Packet);
        tcpBlock.registerReceivedSequenceNumber(rtp4Packet.getSeqNum(), rtp4Packet.getLength());
        sendControl(true, true, false);
        state = RTP4Layer.ConnectionState.SYN_ACCEPTED;
        waitForEstablishment(LISTEN_TIMEOUT_MILLIS);
    }

    private void waitForEstablishment(long timeout) throws TimeoutException {
        long timeStart = System.currentTimeMillis();
        while (state != RTP4Layer.ConnectionState.ESTABLISHED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            if (timeout >= 0 && System.currentTimeMillis() - timeStart > timeout) {
                throw new TimeoutException("Could not create connection within timeout");
            }
        }
    }

    void handlePacket() {
        if (state == RTP4Layer.ConnectionState.CLOSED) {
            return;
        }
        HRP4Packet hrp4Packet = receivedPacketQueue.poll();
        if (hrp4Packet == null) {
            return;
        }
        //Create RTP4 packet from the data, if the data is malformed, return
        RTP4Packet packet;
        try {
            packet = new RTP4Packet(hrp4Packet.getData());
        } catch (PacketMalformedException e) {
            return;
        }
        if (RTP4Layer.DEBUG) System.out.println(Thread.currentThread().getName() + "> " + "Found Packet! " + packet);

        if (tcpBlock.receiveInitialSeqNumIsSet) {
            if (packet.getSeqNum() < tcpBlock.receiveNext) {
                if (RTP4Layer.DEBUG) System.out.println(Thread.currentThread().getName() + "> Nvm, outdated");
                if (packet.getLength() > 0) {
                    sendAcknowledgement();
                }
                return;
            } else if (packet.getSeqNum() > tcpBlock.receiveNext) {
                if (RTP4Layer.DEBUG) System.out.println(Thread.currentThread().getName() + "> Nvm, disordered");
                receivedPacketQueue.add(hrp4Packet);
                return;
            } else {
                tcpBlock.registerReceivedSequenceNumber(packet.getSeqNum(), packet.getLength());
            }
        }

        switch (state) {
            case SYN_ACCEPTED:
                if (packet.isAck()) {
                    receiveAcknowledge(packet);
                }
                if (packet.getData().length > 0) {
                    receivedDataQueue.add(packet.getData());
                }
                if (packet.getLength() > 0) {
                    sendAcknowledgement();
                }
                break;
            case SYN_SENT_1:
                if (packet.isSyn()) {
                    tcpBlock.registerReceivedSequenceNumber(packet.getSeqNum(), packet.getLength());
                    sendAcknowledgement();
                    state = RTP4Layer.ConnectionState.SYN_ACCEPTED;
                }
                if (packet.isAck()) {
                    receiveAcknowledge(packet);
                }
                break;
            case SYN_SENT_2:
                if (packet.isSyn()) {
                    tcpBlock.registerReceivedSequenceNumber(packet.getSeqNum(), packet.getLength());
                    sendAcknowledgement();
                    state = RTP4Layer.ConnectionState.ESTABLISHED;
                }
                if (packet.isAck()) {
                    receiveAcknowledge(packet);
                }
                break;
            case ESTABLISHED:
                if (packet.isFin()) {
                    sendAcknowledgement();
                    state = RTP4Layer.ConnectionState.CLOSE_WAIT;
                } else {
                    if (packet.getData().length > 0) {
                        receivedDataQueue.add(packet.getData());
                    }
                    if (packet.getLength() > 0) {
                        sendAcknowledgement();
                    }
                }
                if (packet.isAck()) {
                    receiveAcknowledge(packet);
                }
                break;
            case FIN_WAIT_1:
                if (packet.isFin()) {
                    state = RTP4Layer.ConnectionState.CLOSING;
                    sendAcknowledgement();
                }
                if (packet.isAck()) {
                    receiveAcknowledge(packet);
                }
                if (packet.getData().length > 0) {
                    receivedDataQueue.add(packet.getData());
                    sendAcknowledgement();
                }
                break;
            case FIN_WAIT_2:
                if (packet.isFin()) {
                    sendAcknowledgement();
                    state = RTP4Layer.ConnectionState.TIME_WAIT;
                    timeWaitStart = System.currentTimeMillis();
                }
                if (packet.isAck()) {
                    receiveAcknowledge(packet);
                }
                if (packet.getData().length > 0) {
                    receivedDataQueue.add(packet.getData());
                    sendAcknowledgement();
                }
                break;
            case CLOSING:
            case CLOSE_WAIT:
            case LAST_ACK:
            case TIME_WAIT:
                if (packet.isAck()) {
                    receiveAcknowledge(packet);
                }
                break;
        }
    }

    void handleAction() {
        RTP4Layer.ConnectionAction action = actionQueue.poll();
        if (action == null) {
            return;
        }
        if (RTP4Layer.DEBUG) System.out.println(Thread.currentThread().getName() + "> " + "Found action! " + action);
        switch (action) {
//            case ACCEPT:
//                HRP4Packet packet;
//                try {
//                    packet = receivedSynQueue.peek();
//                    if (packet != null) {
//                        if (DEBUG) System.out.println(Thread.currentThread().getName() + "> " + "Received Syn!");
//                        receivedSynQueue.remove();
//                        dstAddr = packet.getSrcAddr();
//                        dstPort = packet.getSrcPort();
//                        RTP4Packet rtp4Packet = new RTP4Packet(packet.getData());
//                        tcpBlock.registerReceivedSequenceNumber(rtp4Packet.getSeqNum(),rtp4Packet.getLength());
//                        sendControl(true, true, false);
//                        try {
//                            stateLock.lock();
//                            state = RTP4Layer.ConnectionState.SYN_ACCEPTED;
//                            stateChanged.signal();
//                        } finally {
//                            stateLock.unlock();
//                        }
//                    } else {
//                        if (DEBUG) System.out.println(Thread.currentThread().getName() + "> " + "No Syn yet");
//                        actionQueue.offer(action);
//                    }
//                } catch (PacketMalformedException e) {
//                    e.printStackTrace();
//                }
//                break;
            case CONNECT:
                switch (state){
                    case CLOSED:
                        sendControl(true, false, false);
                        state = RTP4Layer.ConnectionState.SYN_SENT_1;
                        break;
                    case SYN_ACCEPTED:
                        sendControl(true, true, false);
                        state = RTP4Layer.ConnectionState.SYN_SENT_1;
                }
                break;
            case SEND:
                byte[] data = sendDataQueue.poll();
                if (data != null) {
                    sendData(data);
                    unAcknowledgedDataQueue.add(data);
                }
                break;
            case CLOSE:
                switch (state) {
                    case SYN_SENT_1:
                        actionQueue.offer(action);
                        break;
                    case SYN_ACCEPTED:
                    case ESTABLISHED:
                        state = RTP4Layer.ConnectionState.FIN_WAIT_1;
                        sendControl(false, false, true);
                        break;
                    case CLOSE_WAIT:
                        state = RTP4Layer.ConnectionState.LAST_ACK;
                        sendControl(false, false, true);
                }
                break;
        }
    }

    void resendPacket() {
        long time = System.currentTimeMillis();
        AbstractMap.Entry<RTP4Packet, Long> entry;
        while (true) {
            entry = unacknowledgedPacketQueue.peek();
            if (entry == null || time - entry.getValue() < PACKET_TIMEOUT_MILLIS) {
                break;
            }
            if (RTP4Layer.DEBUG) System.out.println(Thread.currentThread().getName() + "> Found Timed out packet! " + entry.getKey());
            unacknowledgedPacketQueue.remove();
            send(entry.getKey());
        }
    }

    private void receiveAcknowledge(RTP4Packet packet) {
        if (tcpBlock.registerReceivedAcknowledgeNumber(packet.getAckNum())) {
            AbstractMap.Entry<RTP4Packet, Long> entry;
            while (true) {
                entry = unacknowledgedPacketQueue.peek();
                if (entry == null) {
                    break;
                }
                if (entry.getKey().getSeqNum() + entry.getKey().getLength() > packet.getAckNum()) {
                    unacknowledgedPacketQueue.offer(entry);
                } else {
                    RTP4Packet acknowledgedPacket = entry.getKey();
                    unacknowledgedPacketQueue.remove();
                    switch (state) {
                        case SYN_ACCEPTED:
                            if (acknowledgedPacket.isSyn()) {
                                state = RTP4Layer.ConnectionState.ESTABLISHED;
                            } else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedPacketQueue but should not be");
                            }
                            break;
                        case SYN_SENT_1:
                            if (acknowledgedPacket.isSyn()) {
                                state = RTP4Layer.ConnectionState.SYN_SENT_2;
                            } else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedPacketQueue but should not be");
                            }
                            break;
                        case CLOSE_WAIT:
                        case ESTABLISHED:
                            unAcknowledgedDataQueue.remove(acknowledgedPacket.getData());
                            break;
                        case FIN_WAIT_1:
                            if (acknowledgedPacket.isFin()) {
                                state = RTP4Layer.ConnectionState.FIN_WAIT_2;
                            } else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedPacketQueue but should not be");
                            }
                            break;
                        case CLOSING:
                            if (acknowledgedPacket.isFin()) {
                                state = RTP4Layer.ConnectionState.TIME_WAIT;
                                timeWaitStart = System.currentTimeMillis();
                            } else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedPacketQueue but should not be");
                            }
                            break;
                        case LAST_ACK:
                            if (acknowledgedPacket.isFin()) {
                                state = RTP4Layer.ConnectionState.CLOSED;
                            } else {
                                System.err.print(acknowledgedPacket.toString() + " is in unacknowledgedPacketQueue but should not be");
                            }
                            break;
                    }
                }
            }
        } else {
            //TODO handle invalid acknowledgement
        }
    }

    private void sendData(byte[] data) {
        send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(data.length), tcpBlock.receiveNext, false, true, false, false, tcpBlock.receiveWindow, data));
    }

    private void send(RTP4Packet packet) {
        try {
            socket.send(packet.marshal(), remoteHost.getAddress(), remoteHost.getPort());
            if (packet.getLength() > 0) {
                unacknowledgedPacketQueue.add(new AbstractMap.SimpleEntry<>(packet, System.currentTimeMillis()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendControl(boolean syn, boolean ack, boolean fin) {
        send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(syn || fin ? 1 : 0), tcpBlock.receiveNext, syn, ack, fin, false, tcpBlock.receiveWindow, new byte[0]));
    }

    private void sendAcknowledgement() {
        send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(0), tcpBlock.receiveNext, false, true, false, false, tcpBlock.receiveWindow, new byte[0]));
    }



    public void send(byte[] data) throws IOException, TimeoutException {
        sendDataQueue.add(data);
        actionQueue.add(RTP4Layer.ConnectionAction.SEND);
        long timeStart = System.currentTimeMillis();
        while (unAcknowledgedDataQueue.contains(data) || sendDataQueue.contains(data)) {
            if (localIsClosed()) {
                throw new IOException("Connection closed while sending");
            }
            if (LISTEN_TIMEOUT_MILLIS >= 0 && System.currentTimeMillis() - timeStart > LISTEN_TIMEOUT_MILLIS) {
                throw new TimeoutException("Timed out while sending");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public byte[] receive() throws InterruptedException, TimeoutException {
        long timeStart = System.currentTimeMillis();
        while (true) {
            byte[] data = receivedDataQueue.poll();
            if (data != null && data.length > 0) {
                return data;
            }
            if (LISTEN_TIMEOUT_MILLIS >= 0 && System.currentTimeMillis() - timeStart > LISTEN_TIMEOUT_MILLIS) {
                throw new TimeoutException("Did not receive packet within timeout");
            }
            if (remoteIsClosed()) {
                return null;
            }
            Thread.sleep(10);

        }
    }

    public boolean localIsClosed(){
        return state == RTP4Layer.ConnectionState.FIN_WAIT_1
                || state == RTP4Layer.ConnectionState.FIN_WAIT_2
                || isClosed();
    }

    public boolean remoteIsClosed(){
        return state == RTP4Layer.ConnectionState.CLOSE_WAIT
                || isClosed();
    }

    public boolean isClosed(){
        return state == RTP4Layer.ConnectionState.TIME_WAIT
                || state == RTP4Layer.ConnectionState.CLOSING
                || state == RTP4Layer.ConnectionState.LAST_ACK
                || state == RTP4Layer.ConnectionState.CLOSED;

    }

    @Override
    public void close() throws IOException {
        actionQueue.offer(RTP4Layer.ConnectionAction.CLOSE);
        try {
            while (state != RTP4Layer.ConnectionState.CLOSED && state != RTP4Layer.ConnectionState.TIME_WAIT) {
                Thread.sleep(100);
            }
            if (state == RTP4Layer.ConnectionState.CLOSED) {
                clear();
            }
        } catch (InterruptedException e) {
            throw new IOException();
        }
    }


    void clear() {
        switch (state) {
            case CLOSED:
                return;
            case TIME_WAIT:
                if (System.currentTimeMillis() - timeWaitStart > Config.getInstance().maxSegmentLife) {
                    state = RTP4Layer.ConnectionState.CLOSED;
                }
        }
    }

    @Override
    public void receive(IPacket packet) {
        if (state != RTP4Layer.ConnectionState.CLOSED && packet instanceof HRP4Packet) {
            receivedPacketQueue.add(((HRP4Packet) packet));
        }
    }


    class TCPBlock {

        private int sendUnacknowledged;
        private int sendNext;
        private short sendWindow;
        private int sendWindowUpdateSeqNum;
        private int sendWindowUpdateAckNum;
        private final int sendInitialSeqNum;

        private int receiveNext;
        private boolean receiveInitialSeqNumIsSet;
        private short receiveWindow;
        private int receiveInitialSeqNum;

        TCPBlock() {
            //TODO reset to non debug init
//            this.sendInitialSeqNum = (int) (System.nanoTime() / 4000);
            this.sendInitialSeqNum = new Random().nextInt(100);
            this.sendNext = this.sendInitialSeqNum;
            this.sendUnacknowledged = this.sendInitialSeqNum - 1;
            this.receiveInitialSeqNumIsSet = false;
        }

        int takeSendSequenceNumber(int length) {
            int sequenceNumberCurrent = this.sendNext;
            this.sendNext += length;
            return sequenceNumberCurrent;
        }

        boolean registerReceivedSequenceNumber(int sequenceNumber, int length) {
            if (receiveNext == sequenceNumber) {
                receiveNext += length;
                return true;
            } else if (!receiveInitialSeqNumIsSet) {
                receiveInitialSeqNum = sequenceNumber;
                receiveInitialSeqNumIsSet = true;
                receiveNext = receiveInitialSeqNum + length;
                return true;
            }
            if (sequenceNumber > receiveNext) {
                return false;
            } else {
                return false;
            }
        }

        public boolean registerReceivedAcknowledgeNumber(int acknowledgeNumber) {
            if (acknowledgeNumber > sendUnacknowledged && acknowledgeNumber <= sendNext) {
                sendUnacknowledged = acknowledgeNumber;
                return true;
            } else {
                return false;
            }
        }

    }

}
