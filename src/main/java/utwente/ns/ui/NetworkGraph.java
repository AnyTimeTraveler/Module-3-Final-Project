package utwente.ns.ui;

import com.sun.deploy.panel.ControlPanel;
import utwente.ns.ip.BCN4Packet;
import utwente.ns.ip.HRP4Router;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author John B. Matthews; distribution per GPL.
 */
public class NetworkGraph extends JPanel {

    private static final Random rnd = new Random();
    private final int WIDE;
    private final int HIGH;
    private final int RADIUS = 35;
    private ControlPanel control = new ControlPanel();
    private int radius = RADIUS;
    private Kind kind = Kind.Circular;
    private List<Node> nodes = new ArrayList<Node>();
    private List<Node> selected = new ArrayList<Node>();
    private List<Edge> edges = new ArrayList<Edge>();
    private Point mousePt;
    private Rectangle mouseRect = new Rectangle();
    private boolean selecting = false;
    private Node[] presetNodes = new Node[]{new Node(new Point(100, 100), RADIUS, Color.BLUE, Kind.Circular), new Node(new Point(200, 100),
            RADIUS,
            Color.GREEN,
            Kind.Circular), new Node(new Point(200, 200),
            RADIUS,
            Color.RED,
            Kind.Circular), new Node(new Point(100,
            200),
            RADIUS,
            Color.YELLOW,
            Kind.Circular)};

    public NetworkGraph() {
        WIDE = 640;
        HIGH = 480;
    }

    public NetworkGraph(JPanel networkGraph) {
        this.setOpaque(true);
        WIDE = networkGraph.getWidth();
        HIGH = networkGraph.getHeight();
        mousePt = new Point(WIDE / 2, HIGH / 2);
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
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 2, 3)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 3, 4)),
                    new HRP4Router.BCN4RoutingEntryWrapper(new BCN4Packet.RoutingEntry((byte) 1, (byte) 1, 4, 1))));
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
        if (selecting) {
            g.setColor(Color.darkGray);
            g.drawRect(mouseRect.x, mouseRect.y, mouseRect.width, mouseRect.height);
        }
    }

    void updateNodes(List<HRP4Router.BCN4RoutingEntryWrapper> routingEntries) {
        int i = 0;
        HashMap<Integer, Node> nodes = new HashMap<>();

        for (HRP4Router.BCN4RoutingEntryWrapper entry : routingEntries) {
            int[] addresses = entry.getBcn4Entry().getAddresses();
            if (!nodes.containsKey(addresses[0])) {
                nodes.put(addresses[0], presetNodes[i++]);
            }
            if (!nodes.containsKey(addresses[1])) {
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
            g.setColor(Color.darkGray);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
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
         * Collected all the selected nodes in list.
         */
        public static void getSelected(List<Node> list, List<Node> selected) {
            selected.clear();
            for (Node n : list) {
                if (n.isSelected()) {
                    selected.add(n);
                }
            }
        }

        /**
         * Select no nodes.
         */
        public static void selectNone(List<Node> list) {
            for (Node n : list) {
                n.setSelected(false);
            }
        }

        /**
         * Select a single node; return true if not already selected.
         */
        public static boolean selectOne(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    if (!n.isSelected()) {
                        Node.selectNone(list);
                        n.setSelected(true);
                    }
                    return true;
                }
            }
            return false;
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
        }

        /**
         * Return this node's location.
         */
        public Point getLocation() {
            return p;
        }

        /**
         * Return true if this node contains p.
         */
        public boolean contains(Point p) {
            return b.contains(p);
        }

        /**
         * Return true if this node is selected.
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Mark this node as selected.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}