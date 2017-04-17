package utwente.ns.ip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import utwente.ns.Util;
import utwente.ns.config.Config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This router is responsible for storing routing entries and figuring out ideal routes from host to host.
 *
 * @author rhbvkleef
 *         Created on 4/10/17
 */
public class HRP4Router {
    /**
     * The default (uncorrected) TTL that shall be used as initialization for routing entries.
     */
    private static final byte DEFAULT_TTL = 100;

    /**
     * The value the TTL shall be multiplied with to get milliseconds of TTL for routing entries.
     */
    private static final int TTL_MULTIPLIER = 32;

    /**
     * The table containing all available point-point (single duplex) connections.
     */
    private Map<Integer, Map<Integer, BCN4RoutingEntryWrapper>> linkTable = new ConcurrentHashMap<>();

    /**
     * Get all single-duplex connections available
     * @return a list of single-duplex BCN4RoutingEntryWrapper.
     */
    public List<BCN4RoutingEntryWrapper> getRoutingEntries() {
        List<BCN4RoutingEntryWrapper> entries = new ArrayList<>();
        linkTable.forEach((m, n) -> n.forEach((o, p) -> entries.add(p)));
        return entries;
    }

    /**
     * Update the routing tables for the given BCN4Packet
     *
     * @param packet containing all routing entries from the sending host
     * @throws UnknownHostException when current (own) address does not resolve (config error)
     */
    void update(BCN4Packet packet) throws UnknownHostException {
        updateTTL();
        // Get the address of this node
        int myAddress = Util.addressToInt(InetAddress.getByName(Config.getInstance().myAddress));

        int neighbour = packet.getHip4Packet().getSrcAddr();

        if (neighbour == myAddress) {
            return;
        }

        int linkcost = 1;

        // Update cost to neighbour
        updateNeighbor(myAddress, neighbour, (byte) linkcost);

        List<BCN4Packet.RoutingEntry> routingEntries = packet.getRoutingTable();
        processDataTable(routingEntries);
    }

    /**
     * Update entries for direct neighbor, used for link detection
     *
     * @param myAddress Own address
     * @param addr Other address
     * @param cost The link-cost of this link
     */
    private void updateNeighbor(int myAddress, int addr, byte cost) {
        processEntry(myAddress, addr, cost, DEFAULT_TTL);
        processEntry(addr, myAddress, cost, DEFAULT_TTL);
    }

    /**
     * Process a list of routing entries sent to me
     * @param table the table of routing entries to be put into the master table
     */
    private void processDataTable(List<BCN4Packet.RoutingEntry> table) {

        int myAddress;
        try {
            myAddress = Util.addressToInt(InetAddress.getByName(Config.getInstance().myAddress));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        for (BCN4Packet.RoutingEntry entry : table) {
            if (entry.getAddresses()[0] == myAddress || entry.getAddresses()[1] == myAddress) continue;
            processEntry(entry.getAddresses()[0], entry.getAddresses()[1], entry.getLinkCost(), entry.getTTL());
            processEntry(entry.getAddresses()[1], entry.getAddresses()[0], entry.getLinkCost(), entry.getTTL());
        }
    }

    /**
     * Process one specific routing entry
     *
     * @param addr1 One endpoint of this connection
     * @param addr2 The other endpoint of this connection
     * @param weight The link-weight of this connection
     * @param ttl The time before expiry of this record
     */
    private void processEntry(int addr1, int addr2, byte weight, byte ttl) {
        if (!linkTable.containsKey(addr1)) {
            linkTable.put(addr1, new HashMap<>());
        }
        Map<Integer, BCN4RoutingEntryWrapper> routes = linkTable.get(addr1);
        if (routes.containsKey(addr2)) {
            BCN4RoutingEntryWrapper route = routes.get(addr2);
            if (route.getRemaining() < TTL_MULTIPLIER * ((int) ttl)) {
                route.setTimeSince(System.currentTimeMillis());
                route.getBcn4Entry().setLinkCost(weight);
                route.getBcn4Entry().setTTL(ttl);
            }
        } else {
            routes.put(addr2, new BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry(weight, ttl, addr1, addr2)));
        }
    }

    /**
     * updateTTL checks expiry of records in the routing table, and removes them.
     */
    private void updateTTL() {
        for (Map.Entry<Integer, Map<Integer, BCN4RoutingEntryWrapper>> node: linkTable.entrySet()) {
            List<Integer> toRemove = new LinkedList<>();
            for (Map.Entry<Integer, BCN4RoutingEntryWrapper> entry: node.getValue().entrySet()) {
                if (entry.getValue().isExpired()) {
                    toRemove.add(entry.getKey());
                }
            }
            for (Integer rem : toRemove) {
                node.getValue().remove(rem);
            }
        }
    }

    /**
     * Calculate the next-hop of a packet given the originator address of the packet
     *
     * @param sourceAddress the address for which the next hops need to be calculated
     * @return Map of destinationAddress --> nextHop
     */
    @SuppressWarnings("ConstantConditions")
    private HashMap<Integer, Integer> dijkstra(int sourceAddress) {
        List<RoutingEntry> closed = new ArrayList<>();
        List<RoutingEntry> open = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        open.add(new RoutingEntry(sourceAddress, -1, 0));

        while(open.size() > 0) {
            RoutingEntry lowest = null;
            for (RoutingEntry entry: open) {
                if (lowest == null || entry.cost < lowest.cost) lowest = entry;
            }
            open.remove(lowest);
            closed.add(lowest);
            visited.add(lowest.destination);

            if (linkTable.get(lowest.destination) == null) {
                continue;
            }

            for (Map.Entry<Integer, BCN4RoutingEntryWrapper> entry: linkTable.get(lowest.destination).entrySet()) {
                int alt = lowest.cost + entry.getValue().getBcn4Entry().getLinkCost();

                int index = -1;
                for (int i = 0; i < open.size(); i++) {
                    if(open.get(i).destination == entry.getKey()) {
                        index = i;
                        break;
                    }
                }
                if (visited.contains(entry.getKey())) {
                    continue;
                }

                if (index == -1) {
                    open.add(new RoutingEntry(entry.getKey(), lowest.hop == -1 ? entry.getKey() : lowest.hop, alt));
                }else if (alt < open.get(index).cost) {
                    open.get(index).cost = alt;
                    open.get(index).hop = lowest.hop;
                    if(lowest.hop == -1) {
                        open.get(index).hop = entry.getKey();
                    }
                }
            }
        }

        HashMap<Integer, Integer> result = new HashMap<>();

        for(RoutingEntry e : closed) {
            if (e.hop != -1) {
                result.put(e.destination, e.hop);
            }
        }

        return result;
    }

    /**
     * Calculate the next-hop of a packet given the originator address of the packet.
     * Also ensures that no old (expired) records are in this list.
     *
     * @param sourceAddress the address for which the next hops need to be calculated
     * @return Map of destinationAddress --> nextHop
     */
    HashMap<Integer, Integer> getForwardingTable(int sourceAddress) {
        updateTTL();
        return dijkstra(sourceAddress);
    }

    /**
     * Storage for routingEntry
     */
    @Data
    @AllArgsConstructor
    private static class RoutingEntry {
        private int destination;
        private int hop;
        private int cost;
    }

    @Data
    @RequiredArgsConstructor
    public static class BCN4RoutingEntryWrapper {
        private final BCN4Packet.RoutingEntry bcn4Entry;
        private long timeSince = System.currentTimeMillis();

        /**
         * Returns the remaining TTL
         *
         * @return the remaining TTL
         */
        long getRemaining() {
            return System.currentTimeMillis() - timeSince - (TTL_MULTIPLIER * bcn4Entry.getTTL());
        }

        /**
         * Gets the adjusted TTL value (should be used over <code>this.getBcn4Entry().getTTL()</code>
         * @return the actual TTL value
         */
        byte getTTL() {
            if (((System.currentTimeMillis() - timeSince) / TTL_MULTIPLIER) > bcn4Entry.getTTL()) {
                return 0;
            }
            return (byte) (bcn4Entry.getTTL() - ((System.currentTimeMillis() - timeSince) / TTL_MULTIPLIER));
        }

        /**
         * Returns <code>this.getTTL() == 0</code>
         * @return <code>this.getTTL() == 0</code>
         */
        boolean isExpired() {
            return this.getTTL() == 0;
            //return (TTL_MULTIPLIER * ((int) bcn4Entry.getTTL())) + timeSince >= System.currentTimeMillis();
        }
    }
}
