package utwente.ns.ip;

import lombok.AllArgsConstructor;
import lombok.Data;
import utwente.ns.IPacket;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;

import java.nio.ByteBuffer;

/**
 * (De)Marshaller class for the HIP4 (transport) layer
 *
 * @author rhbvkleef
 *         Created on 4/7/17
 */
@SuppressWarnings({"unused"})
@Data
@AllArgsConstructor
public class HRP4Packet implements IPacket {
    /**
     * The HIP4 header's length in bytes
     */
    private static final int HEADER_LENGTH = 20; //bytes

    /**
     * Source address: sender's address.
     */
    private int srcAddr;

    /**
     * Destination address: recipient's address.
     */
    private int dstAddr;

    /**
     * Source port: Port mapped to process on sender's host
     */
    private short srcPort;

    /**
     * Destination port: Port mapped to process on recipient's host
     */
    private short dstPort;

    /**
     * Time to live: Maximum remaining hops for the current packet
     */
    private byte TTL;

    /**
     * Data that composes the next layer up (payload)
     */
    private byte[] data;

    /**
     * Construct a HRP4Packet with data passed from one layer down (this also unmarshalls)
     * @param raw; Raw data passed from one layer down
     * @throws PacketMalformedException when packet is too short or contains invalid data
     */
    public HRP4Packet(byte[] raw) throws PacketMalformedException {
        if (raw.length < HEADER_LENGTH) {
            throw new PacketMalformedException("Packet too short");
        }

        ByteBuffer buf = ByteBuffer.wrap(raw);
        if (buf.getInt() != (('H' << 24) | ('I' << 16) | ('P' << 8) | '4')) {
            throw new PacketMalformedException("Invalid packet identifier");
        }

        this.srcAddr = buf.getInt();
        this.dstAddr = buf.getInt();
        this.srcPort = buf.getShort();
        this.dstPort = buf.getShort();
        this.TTL = buf.get();
        buf.getShort();buf.get();
        data = new byte[buf.remaining()];
        buf.get(data);
    }

    /**
     * Convert the current layer to a byte[] to be passed to one layer down
     * @return binary representation of the current packet
     */
    @Override
    public byte[] marshal() {
        byte[] out = new byte[data.length + HEADER_LENGTH];
        out[0] = 'H';
        out[1] = 'I';
        out[2] = 'P';
        out[3] = '4';
        System.arraycopy(Util.intToByteArr(this.srcAddr), 0, out, 4, 4);
        System.arraycopy(Util.intToByteArr(this.dstAddr), 0, out, 8, 4);
        System.arraycopy(Util.shortToByteArr(this.srcPort), 0, out, 12, 2);
        System.arraycopy(Util.shortToByteArr(this.dstPort), 0, out, 14, 2);
        out[16] = this.TTL;
        System.arraycopy(this.data, 0, out, 20, this.data.length);
        return out;
    }

    @Override
    public String getIdent() {
        return "HIP4";
    }
}

//         _____ ___ _   _
//        |  ___|_ _| \ | |
//        | |_   | ||  \| |
//        |  _|  | || |\  |
//        |_|   |___|_| \_|
