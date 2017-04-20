package utwente.ns.tcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import sun.security.util.BitArray;
import utwente.ns.IPacket;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;

import java.nio.ByteBuffer;

/**
 * (De)Marshaller class for the RTP4 layer
 *
 * @author nielsoverkamp
 *         Created on 4/7/17
 */
@Data
@AllArgsConstructor
public class RTP4Packet implements IPacket {
    /**
     * The HIP4 header's length in bytes
     */
    static final int HEADER_LENGTH = 16;

    /**
     * Sequence number: the sequence number of the first byte of the data
     */
    private int seqNum;

    /**
     * Acknowledge number: the sequence number expected next
     */
    private int ackNum;

    /**
     * Synchronise flag: indicates setup of the connection
     */
    private boolean syn;

    /**
     * Acknowledge flag: indicates an acknowledgement
     */
    private boolean ack;

    /**
     * Final flag: indicates the closing of the connection
     */
    private boolean fin;

    //Currently unused
    /**
     * Reset flag: indicates the resetting of the connection
     */
    private boolean rst;

    //Currently unused
    /**
     * The size of the sliding window
     */
    private short windowSize;

    /**
     * Data that composes the next layer up
     */
    private byte[] data;

    /**
     * Construct a RTP4Packet with data passed from one layer down (this also decodes the data)
     *
     * @param raw; Raw data passed from one layer down
     * @throws PacketMalformedException when packet is too short or contains invalid data
     */
    @SuppressWarnings("unused")
    public RTP4Packet(byte[] raw) throws PacketMalformedException {
        ByteBuffer buf = ByteBuffer.wrap(raw);
        if (buf.getInt() != (('R' << 24) | ('T' << 16) | ('P' << 8) | '4')) {
            throw new PacketMalformedException("Invalid packet identifier");
        }
        this.seqNum = buf.getInt();
        this.ackNum = buf.getInt();
        BitArray flagByte = new BitArray(8, new byte[]{buf.get()});
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
     *
     * @return binary representation of the current packet
     */
    public byte[] marshal() {
        byte[] out = new byte[data.length + RTP4Packet.HEADER_LENGTH];
        out[0] = 'R';
        out[1] = 'T';
        out[2] = 'P';
        out[3] = '4';
        System.arraycopy(Util.intToByteArr(this.seqNum), 0, out, 4, 4);
        System.arraycopy(Util.intToByteArr(this.ackNum), 0, out, 8, 4);
        BitArray flags = new BitArray(new boolean[]{this.syn, this.ack, this.fin, this.rst, false, false, false, false});
        System.arraycopy(flags.toByteArray(), 0, out, 12, 1);
        System.arraycopy(Util.shortToByteArr(this.windowSize), 0, out, 14, 2);
        System.arraycopy(this.data, 0, out, RTP4Packet.HEADER_LENGTH, this.data.length);
        return out;
    }

    public int getLength() {
        if (data.length > 0) {
            return data.length;
        } else if (syn || fin || rst) {
            return 1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return "RTP4 <Seq=" + seqNum + (ack ? (", Ack=" + ackNum) : "") + (syn ? ", SYN" : "") + (fin ? ", FIN" : "") + (rst ? ", RST" : "") + ">" + (data.length > 0 ? " [" + new String(data) + "]" : "");
    }
}

//         _____ ___ _   _
//        |  ___|_ _| \ | |
//        | |_   | ||  \| |
//        |  _|  | || |\  |
//        |_|   |___|_| \_|
