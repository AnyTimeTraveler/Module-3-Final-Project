package utwente.ns.ip;

import lombok.Data;
import utwente.ns.IPacket;
import utwente.ns.PacketMalformedException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rhbvkleef
 *         Created on 4/7/17
 */
@SuppressWarnings("unused")
@Data
public class BCN4Packet implements IPacket {

    /**
     * RoutingTable contains all entries that need to be communicated with the remote host.
     */
    private List<RoutingEntry> routingTable;

    /**
     * The encapsulating packet
     */
    private HRP4Packet hip4Packet;

    /**
     * Construct a new BCN4Packet from raw data and its encapsulating {@link HRP4Packet}
     * @param hip4Packet the packet that encapsulates this data
     * @param data that shall be demarshalled into a BCN4Packet
     * @throws PacketMalformedException if the packet format does not match
     */
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

    /**
     * Construct a new BCN4Packet from an encapsulating hip4Packet and a list of routing entries.
     * @param hip4Packet the encapsulating {@link HRP4Packet}
     * @param routingEntries the routing entries that shall be put into this packet.
     */
    public BCN4Packet(HRP4Packet hip4Packet, List<RoutingEntry> routingEntries) {
        this.hip4Packet = hip4Packet;
        this.routingTable = routingEntries;
    }

    /**
     * Create a BCN4Packet from {@link HRP4Router.BCN4RoutingEntryWrapper} and an encapsulating {@link HRP4Packet}
     * @param routingEntries the routing entries that shall be put into this packet.
     * @param hip4Packet the encapsulating {@link HRP4Packet}
     */
    public BCN4Packet(List<HRP4Router.BCN4RoutingEntryWrapper> routingEntries, HRP4Packet hip4Packet) {
        this.hip4Packet = hip4Packet;
        this.routingTable = routingEntries.stream().map(entry -> new RoutingEntry(
                entry.getBcn4Entry().getLinkCost(),
                entry.getTTL(),
                entry.getBcn4Entry().getAddresses()[0],
                entry.getBcn4Entry().getAddresses()[1])
        ).collect(Collectors.toList());
    }

    /**
     * Convert the current packet into a byte[]
     * @return a byte[] that is ready to send over the network.
     */
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
     * @throws UnsupportedOperationException always, this operation is unsupported!
     * @return the content of this packet
     */
    @Override
    public byte[] getData() {
        throw new UnsupportedOperationException("BCN4Packet does not contain data");
    }

    /**
     * Data class that describes a routing entry.
     */
    @Data
    public static class RoutingEntry {
        /**
         * The two endpoint addresses
         */
        private final int[] addresses = new int[2];

        /**
         * The linkcost of this link
         */
        private byte linkCost;

        /**
         * The time until expiration of this link
         */
        private byte TTL;

        /**
         * Demarshall a {@link RoutingEntry} from raw data received from the network.
         * @param data the raw data received from the interwebs.
         */
        @SuppressWarnings("WeakerAccess")
        public RoutingEntry(byte[] data) {
            ByteBuffer buff = ByteBuffer.wrap(data);
            this.linkCost = buff.get();
            buff.getShort();
            this.TTL = buff.get();
            this.addresses[0] = buff.getInt();
            this.addresses[1] = buff.getInt();
        }

        /**
         * Construct a routing entry from linkcost, ttl, and two addresses.
         * @param linkCost the link-cost of this link
         * @param TTL the TTL of this link
         * @param addr0 The first address
         * @param addr1 The second address
         */
        @SuppressWarnings("WeakerAccess")
        public RoutingEntry(byte linkCost, byte TTL, int addr0, int addr1) {
            this.linkCost = linkCost;
            this.TTL = TTL;
            this.addresses[0] = addr0;
            this.addresses[1] = addr1;
        }

        /**
         * Convert this entry to a ready-to-send byte[]
         * @return byte[] that is ready to send and can be demarshalled again into this exact structure.
         */
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

        /**
         * Decrement the TTL by a set amount
         * @param by how much the TTL should be decremented by.
         */
        public void decrementTTL(int by) {
            if (this.TTL < by) {
                this.TTL = 0;
            } else {
                this.TTL -= by;
            }
        }
    }
}
