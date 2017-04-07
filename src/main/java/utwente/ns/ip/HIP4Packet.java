package utwente.ns.ip;

import utwente.ns.IPacket;
import utwente.ns.PacketMalformedException;

import java.nio.ByteBuffer;

/**
 * @author rhbvkleef
 *         Created on 4/7/17
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class HIP4Packet implements IPacket {

    int srcAddr, dstAddr;
    short srcPort, dstPort;
    byte TTL;
    byte[] data;

    public HIP4Packet(int srcAddr, int dstAddr, short srcPort, short dstPort, byte TTL, byte[] data) {
        this.srcAddr = srcAddr;
        this.dstAddr = dstAddr;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.TTL = TTL;
        this.data = data;
    }

    public HIP4Packet(byte[] raw) throws PacketMalformedException {
        ByteBuffer buf = ByteBuffer.wrap(raw);
        buf.getInt();
        this.srcAddr = buf.getInt();
        this.dstAddr = buf.getInt();
        this.srcPort = buf.getShort();
        this.dstPort = buf.getShort();
        this.TTL = buf.get();
        buf.getShort();
        buf.get();
        data = new byte[buf.remaining()];
        buf.get(data);
    }

    public byte[] marshall() {
        byte[] out = new byte[data.length + 20];
        out[0] = 'H';
        out[1] = 'I';
        out[2] = 'P';
        out[3] = '4';
        System.arraycopy(intToByteArr(this.srcAddr), 0, out, 4, 4);
        System.arraycopy(intToByteArr(this.dstAddr), 0, out, 8, 4);
        System.arraycopy(shortToByteArr(this.srcPort), 0, out, 12, 2);
        System.arraycopy(shortToByteArr(this.dstPort), 0, out, 14, 2);
        out[16] = this.TTL;
        System.arraycopy(this.data, 0, out, 20, this.data.length);
        return out;
    }

    private byte[] intToByteArr(int in) {
        return ByteBuffer.allocate(4).putInt(in).array();
    }

    private byte[] shortToByteArr(short in) {
        return ByteBuffer.allocate(2).putShort(in).array();
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }
}

//         _____ ___ _   _
//        |  ___|_ _| \ | |
//        | |_   | ||  \| |
//        |  _|  | || |\  |
//        |_|   |___|_| \_|
