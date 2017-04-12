package utwente.ns.tcp;

import javafx.util.Pair;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;
import utwente.ns.ip.HRP4Packet;
import utwente.ns.ip.HRP4Socket;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static utwente.ns.tcp.RTP4Layer.SocketState;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Socket implements IReceiveListener{

    private static final int QUEUE_CAPACITY = 100;

    private RTP4Layer.SocketState state;
    private ReentrantLock stateLock = new ReentrantLock();
    private Condition stateChanged = stateLock.newCondition();


    private final int port;
    private final HRP4Socket ipSocket;
    private int dstAddr;
    private short dstPort;


    Queue<HRP4Packet> receivedPacketQueue;
    BlockingQueue<HRP4Packet> receivedSynQueue;
    Queue<Pair<RTP4Packet,Long>> unacknowledgedQueue;

    private TCPBlock tcpBlock = new TCPBlock();


    public RTP4Socket(HRP4Socket ipSocket) {
        this.port = ipSocket.dstPort;
        this.ipSocket = ipSocket;
        ipSocket.addReceiveListener(this);
        state = SocketState.LISTEN;
        tcpBlock = new TCPBlock();
        receivedPacketQueue = new LinkedList<>();
        receivedSynQueue = new LinkedBlockingQueue<>(10);
        unacknowledgedQueue = new LinkedList<>();
    }

    public RTP4Connection accept() throws IOException {
        HRP4Packet packet;
        try {
            packet = receivedSynQueue.take();
            sendControl(true,true,false);
            try {
                stateLock.lock();
                state = SocketState.SYN_ACCEPTED;
                stateChanged.signal();
            } finally {
                stateLock.unlock();
            }
            try {
                stateLock.lock();
                while (state != SocketState.ESTABLISHED || state != SocketState.CLOSED) {
                    stateChanged.await();
                }
                if (state == SocketState.CLOSED) {
                    throw new IOException("Socket got closed");
                }
            } finally {
                stateLock.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        dstAddr = packet.getSrcAddr();
        dstPort = packet.getSrcPort();
        return new RTP4Connection(packet.getSrcAddr(), ((int) packet.getSrcPort()), this);
    }

    public RTP4Connection connect(String address, int port) throws IOException {
        dstAddr = Util.addressStringToInt(address);
        dstPort = (short) port;
        sendControl(true,false,false);
        try {
            stateLock.lock();
            state = SocketState.SYN_SENT;
            stateChanged.signal();
        } finally {
            stateLock.unlock();
        }
        try {
            stateLock.lock();
            while (state != SocketState.ESTABLISHED || state != SocketState.CLOSED) {
                stateChanged.await();
            }
            if (state == SocketState.CLOSED) {
                throw new IOException("Socket got closed");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            stateLock.unlock();
        }
        return new RTP4Connection(dstAddr,dstPort,this);
    }

    void handle(HRP4Packet ipPacket) {
        RTP4Packet packet = null;
        try {
            packet = new RTP4Packet(ipPacket.getData());
        } catch (PacketMalformedException e) {
            return;
        }
        stateLock.lock();
        try {
            switch (state) {
                case CLOSED:
                    sendReset(packet);
                    break;
                case LISTEN:
                    if (packet.isSyn()) {
                        receivedSynQueue.offer(ipPacket);
                    }
                    break;
                case SYN_ACCEPTED:
                    if (packet.isAck()) {
                        //TODO Check packet and update TCPBlock
                        //TODO Pass on data
                        state = SocketState.ESTABLISHED;
                    } else {
                        //TODO handle, probably sendReset or resend SYNACK
                    }
                    break;
                case SYN_SENT:
                    if (packet.isSyn()) {
                        if (packet.isAck()) {
                            //TODO Check packet and update TCPBlock
                            //TODO Send data instead of:
                            send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(1), tcpBlock.receiveNext, false, true, false, false, tcpBlock.receiveWindow, new byte[0]));
                            state = SocketState.ESTABLISHED;
                        } else {
                            send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(1), tcpBlock.receiveNext, false, true, false, false, tcpBlock.receiveWindow, new byte[0]));
                            state = SocketState.SYN_ACCEPTED;
                        }
                    } else {
                        //TODO handle, probably sendReset or resend SYN
                    }
                    break;
                case ESTABLISHED:
                    if (packet.isFin()) {
                        //TODO Check packet and update TCPBlock ?
                        state = SocketState.CLOSE_WAIT;
                    } else {
                        //TODO Pass on data
                    }
                    break;
                case FIN_WAIT_1:
                    if (packet.isFin()) {
                        //TODO Check packet and update TCPBlock ?
                        state = SocketState.CLOSING;
                    } else if (packet.isAck()) {
                        //TODO Check packet and update TCPBlock
                        state = SocketState.FIN_WAIT_2;
                    } else {
                        //TODO handle, probably sendReset, ignore or resend a packet
                    }
                    break;
                case FIN_WAIT_2:
                    if (packet.isFin()) {
                        state = SocketState.TIME_WAIT;
                    } else {
                        //TODO handle, probably sendReset, ignore or resend a packet
                    }
                    break;
                case CLOSING:
                    if (packet.isAck()) {
                        state = SocketState.TIME_WAIT;
                    } else {
                        //TODO handle, probably sendReset, ignore or resend a packet
                    }
                    break;
                case TIME_WAIT:
                    //TODO handle, probably sendReset, ignore or resend a packet
                    break;
                case CLOSE_WAIT:
                    //TODO handle, probably sendReset, ignore or resend a packet
                    break;
                case LAST_ACK:
                    if (packet.isAck()) {
                        //TODO Check packet and update TCPBlock
                        state = SocketState.CLOSED;
                    }
                    break;
            }
            stateChanged.signal();
        } finally {
            stateLock.unlock();
        }
    }



    private void sendReset(RTP4Packet receivedPacket) {
        RTP4Packet resetPacket = new RTP4Packet(receivedPacket.getAckNum(), receivedPacket.getSeqNum() + receivedPacket.getData().length,
                false, false, false, true, (short) 0, new byte[0]);
        send(resetPacket);

    }


    void send(byte[] data) {
        send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(data.length), tcpBlock.receiveNext, false, false, false, false, (short) 0, data));
    }


    private void send(RTP4Packet packet) {
        try {
            ipSocket.send(packet.marshal(), dstAddr, dstPort);
            unacknowledgedQueue.add(new Pair<RTP4Packet,Long>(packet,System.currentTimeMillis()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendControl(boolean syn, boolean ack, boolean fin) {
        send(new RTP4Packet(tcpBlock.takeSendSequenceNumber(1), tcpBlock.receiveNext, syn, ack, fin, false, tcpBlock.receiveWindow, new byte[0]));
    }

    @Override
    public void receive(IPacket packet) {
        if (state != SocketState.CLOSED) {
            receivedPacketQueue.offer((HRP4Packet) packet);
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
        private short receiveWindow;
        private int receiveInitialSeqNum;

        public TCPBlock() {
            this.sendInitialSeqNum = (int) (System.nanoTime() / 4000);
            this.sendNext = this.sendInitialSeqNum;
            this.sendUnacknowledged = this.sendInitialSeqNum - 1;
        }

        public int takeSendSequenceNumber(int length) {
            int sequenceNumberCurrent = this.sendNext;
            this.sendNext += length;
            return sequenceNumberCurrent;
        }

        public boolean registerReceiveSequenceNumber(int sequenceNumber, int length) {
            if (receiveNext == sequenceNumber) {
                receiveNext += length;
                return true;
            } else if (sequenceNumber > receiveNext) {
                return false;
            } else {
                return false;
            }
        }

        public boolean registerReceivedAcknowledgeNumber(int acknowledgeNumber) {
            if (acknowledgeNumber > sendUnacknowledged && acknowledgeNumber <= sendNext) {
                sendUnacknowledged = acknowledgeNumber + 1;
                return true;
            } else {
                return false;
            }
        }

    }

}
