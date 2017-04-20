package utwente.ns.ui;

import utwente.ns.Util;
import utwente.ns.ip.BCN4Packet;
import utwente.ns.ip.HRP4Router;

import javax.swing.*;
import java.awt.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author John B. Matthews; distribution per GPL.
 */
public class NetworkGraph extends JPanel {

    public static final int LINE_WIDTH = 5;
    private final int WIDE;
    private final int HIGH;
    private final int RADIUS = 50;
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private Node[] presetNodes = new Node[]{
            new Node(new Point(150, 150), RADIUS, Color.BLUE, Kind.Circular),
            new Node(new Point(300, 150), RADIUS, Color.GREEN, Kind.Circular),
            new Node(new Point(300, 300), RADIUS, Color.RED, Kind.Circular),
            new Node(new Point(150, 300), RADIUS, Color.YELLOW, Kind.Circular)
    };

    public NetworkGraph() {
        WIDE = 640;
        HIGH = 480;
    }

    public NetworkGraph(JPanel networkGraph) {
        this.setOpaque(true);
        WIDE = networkGraph.getWidth();
        HIGH = networkGraph.getHeight();
    }

    public static void main() throws Exception {
        EventQueue.invokeLater(() -> {
            JFrame f = new JFrame("GraphPanel");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            NetworkGraph gp = new NetworkGraph();
            f.add(new JScrollPane(gp), BorderLayout.CENTER);
            f.pack();
            f.setLocationByPlatform(true);
            f.setVisible(true);

            gp.updateNodes(Arrays.asList(new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 1, 2)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 2, 1)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 2, 3)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 3, 2)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 1, 3)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 3, 1)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 1, 4)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 2, 4)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 3, 4)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 4, 1)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 4, 2)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 4, 3))
            ));
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDE, HIGH);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(0x00f0f0f0));
        g.fillRect(0, 0, getWidth(), getHeight());
        for (Edge e : edges) {
            e.draw(g);
        }
        for (Node n : nodes) {
            n.draw(g);
        }
    }

    void updateNodes(List<HRP4Router.BCN4RoutingEntryWrapper> routingEntries) {
        int i = 0;
        HashMap<Integer, Node> nodes = new HashMap<>();

        this.nodes.clear();
        edges.clear();
        for (HRP4Router.BCN4RoutingEntryWrapper entry : routingEntries) {
            int[] addresses = entry.getBcn4Entry().getAddresses();
            if (i < 4 && !nodes.containsKey(addresses[0])) {
                try {
                    presetNodes[i].text = Util.intToAddressString(addresses[0]);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                nodes.put(addresses[0], presetNodes[i++]);
            }
            if (i < 4 && !nodes.containsKey(addresses[1])) {
                try {
                    presetNodes[i].text = Util.intToAddressString(addresses[1]);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                nodes.put(addresses[1], presetNodes[i++]);
            }
            edges.add(new Edge(nodes.get(addresses[0]), nodes.get(addresses[1])));
        }
        this.nodes.addAll(nodes.values());

        /*
        // Random
        Point p = new Point(rnd.nextInt(getWidth()), rnd.nextInt(getHeight()));
        this.nodes.add(new Node(p, radius, new Color(rnd.nextInt()), kind));
        */
    }

    /**
     * The kinds of node in a graph.
     */
    private enum Kind {
        Circular, Rounded, Square;
    }

    /**
     * An Edge is a pair of Nodes.
     */
    private static class Edge {

        private Node n1;
        private Node n2;

        public Edge(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
        }

        public void draw(Graphics g) {
            Point p1 = n1.getLocation();
            Point p2 = n2.getLocation();
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(n1.color);
            g2d.setStroke(new BasicStroke(LINE_WIDTH));
//            System.out.println(n1.text + " --> " + n2.text);
            int x1 = p1.x;
            int y1 = p1.y;
            int x2 = p2.x;
            int y2 = p2.y;

            if (p1.getY() == p2.getY()) {
                int mody = p1.getX() < p2.getX() ? -LINE_WIDTH : LINE_WIDTH;
                y1 += mody;
                y2 += mody;
            } else/* if (p1.getX() == p2.getX())*/ {
                int modx = p1.getY() < p2.getY() ? -LINE_WIDTH : LINE_WIDTH;
                x1 += modx;
                x2 += modx;
            }
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * A Node represents a node in a graph.
     */
    private static class Node {

        private Point p;
        private int r;
        private Color color;
        private Kind kind;
        private boolean selected = false;
        private Rectangle b = new Rectangle();
        private String text;

        /**
         * Construct a new node.
         */
        public Node(Point p, int r, Color color, Kind kind) {
            this.p = p;
            this.r = r;
            this.color = color;
            this.kind = kind;
            setBoundary(b);
        }

        /**
         * Calculate this node's rectangular boundary.
         */
        private void setBoundary(Rectangle b) {
            b.setBounds(p.x - r, p.y - r, 2 * r, 2 * r);
        }

        /**
         * Draw this node.
         */
        public void draw(Graphics g) {
            g.setColor(this.color);
            if (this.kind == Kind.Circular) {
                g.fillOval(b.x, b.y, b.width, b.height);
            } else if (this.kind == Kind.Rounded) {
                g.fillRoundRect(b.x, b.y, b.width, b.height, r, r);
            } else if (this.kind == Kind.Square) {
                g.fillRect(b.x, b.y, b.width, b.height);
            }
            if (selected) {
                g.setColor(Color.darkGray);
                g.drawRect(b.x, b.y, b.width, b.height);
            }
            g.setColor(Color.BLACK);
            g.setFont(new Font("default", Font.BOLD, 15));
            if (text != null)
                g.drawString(text, b.x + r / 4, b.y + r / 2 + r / 3);
        }

        /**
         * Return this node's location.
         */
        public Point getLocation() {
            return p;
        }
    }
}