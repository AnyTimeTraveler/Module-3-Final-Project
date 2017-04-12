package utwente.ns.ip;

import lombok.Data;
import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.PacketMalformedException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rhbvkleef
 *         Created on 4/7/17
 */
@SuppressWarnings("unused")
@Data
public class BCN4Packet implements IPacket {

    private List<RoutingEntry> routingTable;
    @Getter
    private HRP4Packet hip4Packet;

    public BCN4Packet(HRP4Packet hip4Packet, byte[] data) throws PacketMalformedException {
        this.hip4Packet = hip4Packet;

        routingTable = new ArrayList<>();

        if (data.length < 4 || (data.length - 4) % 12 != 0) {
            throw new PacketMalformedException("Invalid packet size");
        }

        if (data[0] != 'B' || data[1] != 'C' || data[2] != 'N' || data[3] != '4') {
            throw new PacketMalformedException("Invalid packet identifier");
        }

        for (int i = 4; i < data.length; i += 12) {
            routingTable.add(new RoutingEntry(Arrays.copyOfRange(data, i, i + 12)));
        }
    }

    public BCN4Packet(HRP4Packet hip4Packet, List<RoutingEntry> routingEntries) {
        this.hip4Packet = hip4Packet;
        this.routingTable = routingEntries;
    }

    public byte[] marshal() {
        ByteBuffer buf = ByteBuffer.allocate(4 + (12 * routingTable.size()));
        buf.put((byte) 'B');
        buf.put((byte) 'C');
        buf.put((byte) 'N');
        buf.put((byte) '4');
        for (RoutingEntry entry : this.routingTable) {
            buf.put(entry.marshal());
        }
        return buf.array();
    }

    /**
     *
     * @return
     */
    @Override
    public byte[] getData() {
        throw new UnsupportedOperationException("BCN4Packet does not contain data");
    }

    @Data
    public static class RoutingEntry {

        private final int[] addresses = new int[2];
        private byte linkCost;
        private byte TTL;

        @SuppressWarnings("WeakerAccess")
        public RoutingEntry(byte[] data) {
            ByteBuffer buff = ByteBuffer.wrap(data);
            this.linkCost = buff.get();
            buff.getShort();
            this.TTL = buff.get();
            this.addresses[0] = buff.getInt();
            this.addresses[1] = buff.getInt();
        }

        @SuppressWarnings("WeakerAccess")
        public RoutingEntry(byte linkCost, byte TTL, int addr0, int addr1) {
            this.linkCost = linkCost;
            this.TTL = TTL;
            this.addresses[0] = addr0;
            this.addresses[1] = addr1;
        }

        @SuppressWarnings("WeakerAccess")
        public byte[] marshal() {
            ByteBuffer raw = ByteBuffer.allocate(12);
            raw.put(linkCost);
            raw.put((byte) 0);
            raw.put((byte) 0);
            raw.put(TTL);
            raw.putInt(addresses[0]);
            raw.putInt(addresses[1]);

            return raw.array();
        }
    }
}
