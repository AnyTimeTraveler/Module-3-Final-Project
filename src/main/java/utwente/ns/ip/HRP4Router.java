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
 * @author rhbvkleef
 *         Created on 4/10/17
 */
public class HRP4Router {

    private static final byte DEFAULT_TTL = 100;
    private static final int TTL_MULTIPLIER = 16;

    private HRP4Layer ipLayer;

    private Map<Integer, Map<Integer, BCNRoutingEntryAlternative>> linkTable = new ConcurrentHashMap<>();
    private Set<Integer> neighbors = new HashSet<>();

    public HRP4Router(HRP4Layer ipLayer) {
        this.ipLayer = ipLayer;
    }

    public List<BCN4Packet.RoutingEntry> getRoutingEntries() {
        List<BCN4Packet.RoutingEntry> entries = new ArrayList<>();
        linkTable.forEach((m, n) -> n.forEach((o, p) -> entries.add(p.getBcn4Entry())));
        return entries;
    }

    public synchronized void update(BCN4Packet packet) throws UnknownHostException {
        // Get the address of this node
        int myAddress = Util.addressToInt(InetAddress.getByName(Config.getInstance().getMyAddress()));

        int neighbour = packet.getHip4Packet().getSrcAddr();

        if (neighbour == myAddress) {
            return;
        }

        int linkcost = 1;
        neighbors.add(neighbour);

        // Update cost to neighbour
        updateNeighbor(myAddress, neighbour, (byte) linkcost);

        List<BCN4Packet.RoutingEntry> routingEntries = packet.getRoutingTable();
        processDataTable(routingEntries);
    }

    private void updateNeighbor(int myAddress, int addr, byte cost) {
        processEntry(myAddress, addr, cost, DEFAULT_TTL);
        processEntry(addr, myAddress, cost, DEFAULT_TTL);
    }

    private void processDataTable(List<BCN4Packet.RoutingEntry> table) {

        int myAddress;
        try {
            myAddress = Util.addressToInt(InetAddress.getByName(Config.getInstance().getMyAddress()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < table.size(); i++) {
            BCN4Packet.RoutingEntry entry = table.get(i);
            if (entry.getAddresses()[0] ==  myAddress || entry.getAddresses()[1] == myAddress) continue;
            processEntry(entry.getAddresses()[0], entry.getAddresses()[1], entry.getLinkCost(), entry.getTTL());
            processEntry(entry.getAddresses()[1], entry.getAddresses()[0], entry.getLinkCost(), entry.getTTL());
        }
    }

    private void processEntry(int addr1, int addr2, byte weight, byte ttl) {
        if (!linkTable.containsKey(addr1)) {
            linkTable.put(addr1, new HashMap<>());
        }
        Map<Integer, BCNRoutingEntryAlternative> routes = linkTable.get(addr1);
        if (routes.containsKey(addr2)) {
            BCNRoutingEntryAlternative route = routes.get(addr2);
            if (route.getRemaining() < TTL_MULTIPLIER * ((int) ttl)) {
                route.setTimeSince(System.currentTimeMillis());
                route.getBcn4Entry().setLinkCost(weight);
                route.getBcn4Entry().setTTL(ttl);
            }
        } else {
            routes.put(addr2, new BCNRoutingEntryAlternative(new BCN4Packet.RoutingEntry(weight, ttl, addr1, addr2)));
        }
    }

    private void updateTTL() {
        long currentTimeMillis = System.currentTimeMillis();
        for (Map.Entry<Integer, Map<Integer, BCNRoutingEntryAlternative>> node: linkTable.entrySet()) {
            List<Integer> toRemove = new LinkedList<>();
            for (Map.Entry<Integer, BCNRoutingEntryAlternative> entry: node.getValue().entrySet()) {
                entry.getValue().bcn4Entry.decrementTTL((int) (currentTimeMillis - entry.getValue().timeSince)/TTL_MULTIPLIER);

                if (entry.getValue().isExpired()) {
                    toRemove.add(entry.getKey());
                }
            }
            System.out.printf("Removing: %s\n", toRemove.toString());
            for (Integer rem : toRemove) {
                node.getValue().remove(rem);
            }
        }
    }

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

            for (Map.Entry<Integer, BCNRoutingEntryAlternative> entry: linkTable.get(lowest.destination).entrySet()) {
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

    public HashMap<Integer, Integer> getForwardingTable(int sourceAddress) {
        updateTTL();
        return dijkstra(sourceAddress);
    }

    @Data
    @AllArgsConstructor
    public static class RoutingEntry {
        private int destination;
        private int hop;
        private int cost;
    }

    @Data
    @RequiredArgsConstructor
    public static class BCNRoutingEntryAlternative {
        private final BCN4Packet.RoutingEntry bcn4Entry;
        private long timeSince = System.currentTimeMillis();

        public long getRemaining() {
            return System.currentTimeMillis() - timeSince - (TTL_MULTIPLIER * bcn4Entry.getTTL());
        }

        public boolean isExpired() {
            return (TTL_MULTIPLIER * ((int) bcn4Entry.getTTL())) + timeSince >= System.currentTimeMillis();
        }
    }
}
