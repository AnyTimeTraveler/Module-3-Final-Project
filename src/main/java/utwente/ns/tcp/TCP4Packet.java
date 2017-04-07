package utwente.ns.tcp;

import lombok.Data;
import utwente.ns.IPacket;
import utwente.ns.PacketMalformedException;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * (De)Marshaller class for the TCP4 layer
 *
 * @author nielsoverkamp
 *         Created on 4/7/17
 */
@SuppressWarnings({"unused"})
@Data
public class TCP4Packet implements IPacket{
    /**
     * The HIP4 header's length in bytes
     */
    static final int HEADER_LENGTH = 16;

    /**
     * Sequence number: the sequence number of the first byte of the data
     */
    int seqNum;

    /**
     * Acknowledge number: the sequence number expected next
     */
    int ackNum;

    /**
     * Synchronise flag: indicates setup of the connection
     */
    boolean syn;

    /**
     * Acknowledge flag: indicates an acknowledgement
     */
    boolean ack;

    /**
     * Final flag: indicates the closing of the connection
     */
    boolean fin;

    /**
     * Reset flag: indicates the resetting of the connection
     */
    boolean rst;

    /**
     * The size of the sliding window
     */
    short windowSize;

    /**
     * Data that composes the next layer up
     */
    byte[] data;

    /**
     * Constructs a TCP4Packet from raw data
     *
     * @param seqNum: Sequence number
     * @param ackNum: Acknowledgement number
     * @param syn: Synchronise flag
     * @param ack: Acknowledgement flag
     * @param fin: Final flag
     * @param rst: Reset flag
     * @param windowSize: Sliding window size
     * @param data: Data that will be passed to/came from the next layer up
     */
    public TCP4Packet(int seqNum, int ackNum,
                      boolean syn, boolean ack, boolean fin, boolean rst, short windowSize, byte[] data) {
        this.seqNum = seqNum;
        this.ackNum = ackNum;
        this.syn = syn;
        this.ack = ack;
        this.fin = fin;
        this.rst = rst;
        this.windowSize = windowSize;
        this.data = data;
    }


    /**
     * Construct a TCP4Packet with data passed from one layer down (this also unmarshalls)
     * @param raw; Raw data passed from one layer down
     * @throws PacketMalformedException when packet is too short or contains invalid data
     */
    public TCP4Packet(byte[] raw) throws PacketMalformedException {
        ByteBuffer buf = ByteBuffer.wrap(raw);
        buf.getInt();
        this.seqNum = buf.getInt();
        this.ackNum = buf.getInt();
        BitSet flagByte = BitSet.valueOf(new byte[]{buf.get()});
        this.syn = flagByte.get(0);
        this.ack = flagByte.get(1);
        this.fin = flagByte.get(2);
        this.rst = flagByte.get(3);
        buf.get();
        this.windowSize = buf.getShort();
        data = new byte[buf.remaining()];
        buf.get(data);
    }

    /**
     * Convert the current layer to a byte[] to be passed to one layer down
     * @return binary representation of the current packet
     */
    public byte[] marshal() {
        byte[] out = new byte[data.length + TCP4Packet.HEADER_LENGTH];
        out[0] = 'T';
        out[1] = 'C';
        out[2] = 'P';
        out[3] = '4';
        System.arraycopy(intToByteArr(this.seqNum), 0, out, 4, 4);
        System.arraycopy(intToByteArr(this.ackNum), 0, out, 8, 4);
        BitSet flags = new BitSet(8);
        flags.set(0,this.syn);
        flags.set(1,this.ack);
        flags.set(2,this.fin);
        flags.set(3,this.rst);
        System.arraycopy(flags.toByteArray(), 0, out, 12, 1);
        System.arraycopy(shortToByteArr(this.windowSize), 0, out, 14, 2);
        System.arraycopy(this.data, 0, out, TCP4Packet.HEADER_LENGTH, this.data.length);
        return out;
    }

    private byte[] intToByteArr(int in) {
        return ByteBuffer.allocate(4).putInt(in).array();
    }

    private byte[] shortToByteArr(short in) {
        return ByteBuffer.allocate(2).putShort(in).array();
    }
}

//         _____ ___ _   _
//        |  ___|_ _| \ | |
//        | |_   | ||  \| |
//        |  _|  | || |\  |
//        |_|   |___|_| \_|
