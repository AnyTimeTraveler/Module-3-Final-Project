package utwente.ns.tcp;

import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.ip.HRP4Packet;

import java.io.Closeable;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Connection implements Closeable, IReceiveListener{
    @Getter
    private RemoteHost remoteHost;
    @Getter
    private final RTP4Socket socket;

    BlockingQueue<AbstractMap.Entry<RTP4Packet, Long>> unacknowledgedQueue;
    private BlockingQueue<byte[]> receivedDataQueue;
    BlockingQueue<byte[]> sendDataQueue;

    private RTP4Layer.ConnectionState state;



    private BlockingQueue<HRP4Packet> receivedPacketQueue;
    private BlockingQueue<RTP4Layer.ConnectionAction> actionQueue;
    private TCPBlock tcpBlock;

    RTP4Connection(RemoteHost remoteHost, RTP4Socket socket){
        this.remoteHost = remoteHost;
        this.socket = socket;
        this.receivedDataQueue = new LinkedBlockingQueue<>();
        this.sendDataQueue = new LinkedBlockingQueue<>();
        this.actionQueue = new LinkedBlockingQueue<>();
        receivedPacketQueue = new LinkedBlockingQueue<>();
        actionQueue.offer(RTP4Layer.ConnectionAction.CONNECT);
    }

    RTP4Connection(int address, short port, RTP4Socket socket) {
        this(new RemoteHost(address, port), socket);
    }


    void handle(HRP4Packet ipPacket) {
        if (state == RTP4Layer.ConnectionState.CLOSED || state == RTP4Layer.ConnectionState.TIME_WAIT) {
            return;
        }
        //Create RTP4 packet from the data, if the dat is malformed, return
        RTP4Packet packet;
        try {
            packet = new RTP4Packet(ipPacket.getData());
        } catch (PacketMalformedException e) {
            return;
        }

        if (tcpBlock.receiveInitialSeqNumIsSet) {
            if (packet.getSeqNum() < tcpBlock.receiveNext ) {
                System.out.println(Thread.currentThread().getName() + "> Nvm, outdated");
                if (packet.getLength() > 0) {
                    sendAcknowledgement();
                }
                return;
            } else if (packet.getSeqNum() > tcpBlock.receiveNext) {
                System.out.println(Thread.currentThread().getName() + "> Nvm, disordered");
                receivedPacketQueue.add(ipPacket);
                return;
            } else {
                tcpBlock.registerReceivedSequenceNumber(packet.getSeqNum(), packet.getLength());
            }
        }

        stateLock.lock();
        try {
            switch (state) {
                case LISTEN:
                    if (packet.isSyn()) {
                        receivedSynQueue.offer(ipPacket);
                    }
                    break;
                case SYN_ACCEPTED:
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
                case SYN_SENT:
                    if (packet.isSyn()) {
                        tcpBlock.registerReceivedSequenceNumber(packet.getSeqNum(), packet.getLength());
                        sendAcknowledgement();
                        state = RTP4Layer.ConnectionState.SYN_ACCEPTED;
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
                            listeningConnections.forEach(connection -> connection.receiveData(packet.getData()));
                        }
                        if (packet.getLength() > 0){
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
                    break;
                case CLOSING:
                case CLOSE_WAIT:
                case LAST_ACK:
                    if (packet.isAck()) {
                        receiveAcknowledge(packet);
                    }
                    break;
            }
            stateChanged.signal();
        } finally {
            stateLock.unlock();
        }
    }

    void handle(RTP4Layer.ConnectionAction action){
        switch (action) {
//            case ACCEPT:
//                HRP4Packet packet;
//                try {
//                    packet = receivedSynQueue.peek();
//                    if (packet != null) {
//                        System.out.println(Thread.currentThread().getName() + "> " + "Received Syn!");
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
//                        System.out.println(Thread.currentThread().getName() + "> " + "No Syn yet");
//                        actionQueue.offer(action);
//                    }
//                } catch (PacketMalformedException e) {
//                    e.printStackTrace();
//                }
//                break;
            case CONNECT:
                sendControl(true, false, false);
                state = RTP4Layer.ConnectionState.SYN_SENT;
                break;
            case CLOSE:
                switch (state) {
                    case LISTEN:
                    case SYN_SENT:
                        actionQueue.offer(action);
                        break;
                    case SYN_ACCEPTED:
                    case ESTABLISHED:
                        state = RTP4Layer.ConnectionState.FIN_WAIT_1;
                        sendControl(false,false,true);
                        break;
                    case CLOSE_WAIT:
                        state = RTP4Layer.ConnectionState.LAST_ACK;
                        sendControl(false,false,true);
                }
                break;
        }
    }

    private void send(RTP4Packet packet) {
        try {
            socket.send(packet.marshal(), remoteHost.getAddress(), remoteHost.getPort());
            if (packet.getLength() > 0) {
                unacknowledgedQueue.add(new AbstractMap.SimpleEntry<>(packet, System.currentTimeMillis()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendControl(boolean syn, boolean ack, boolean fin) {
        send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(syn || fin ? 1 : 0), tcpBlock.receiveNext, syn, ack, fin, false, tcpBlock.receiveWindow, new byte[0]));
    }

    public void send(byte[] data){
        //TODO fix
        sendDataQueue.add(data);
    }

    public byte[] receive() throws InterruptedException {
        //TODO fix
        return receivedDataQueue.take();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    void receiveData(byte[] data) {
        receivedDataQueue.add(data);
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
            }  else if (!receiveInitialSeqNumIsSet){
                receiveInitialSeqNum = sequenceNumber;
                receiveInitialSeqNumIsSet = true;
                receiveNext = receiveInitialSeqNum + length;
                return true;
            } if (sequenceNumber > receiveNext) {
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
