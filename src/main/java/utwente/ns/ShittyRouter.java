package utwente.ns;

import utwente.ns.config.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * Created by simon on 10.04.17.
 */
public class ShittyRouter {
    private static DatagramSocket socket;
    private static InetAddress mcastaddr;
    private static int port;
    
    public static void main(String[] args) throws Exception {
        mcastaddr = InetAddress.getByName(Config.getInstance().getMulticastAddress());
//        mcastaddr = InetAddress.getByName("224.0.0.251");
        port = Config.getInstance().getMulticastPort();
        socket = new DatagramSocket(port, mcastaddr);
        socket.connect(mcastaddr, port);
        socket.setBroadcast(true);
        Thread receiver = new Thread(ShittyRouter::runReceiver);
        receiver.start();
        Scanner sc = new Scanner(System.in);
        boolean running = true;
        while (running) {
            System.out.println("Started! Local address: " + socket.getLocalAddress());
            String line = sc.nextLine();
            String message = line;
            InetAddress dstAddress = null;
            if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("/exit")) {
                running = false;
                message = "Bye! I'm leaving.";
            }
            if (line.startsWith("/msg")) {
                if (line.split(" ").length < 3) {
                    System.err.println("Usage: /msg [IP Address] Message");
                }
                dstAddress = InetAddress.getByName(line.split(" ")[1]);
                message = line.substring(line.indexOf(' ', 5) + 1);
            }
            byte[] packet = marshal(socket.getLocalAddress(), dstAddress, message.getBytes());
            socket.send(new DatagramPacket(packet, packet.length, mcastaddr, port));
        }
    }
    private static void runReceiver() {
        try {
            byte[] data = new byte[2048];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            Packet receivedPacket = getPacket(data, packet.getLength());
            if (receivedPacket.dst != null && receivedPacket.dst.equals(socket.getLocalAddress())) {
                System.out.println(new String(receivedPacket.data));
            } else if (!receivedPacket.src.equals(socket.getLocalAddress())) {
                System.out.println("Forwarding: " + new String(receivedPacket.data));
                packet.setAddress(mcastaddr);
                packet.setPort(port);
                socket.send(packet);
            } else {
                System.out.println("Sent: " + new String(receivedPacket.data));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static Packet getPacket(byte[] input, int length) throws UnknownHostException {
        Packet p = new Packet();
        ByteBuffer buf = ByteBuffer.wrap(input, 0, length);
        int srcLength = buf.getInt();
        if (srcLength > 0) {
            byte[] srcData = new byte[srcLength];
            buf.get(srcData, 0, srcLength);
            p.src = InetAddress.getByAddress(srcData);
        }
        int dstLength = buf.getInt();
        if (dstLength > 0) {
            byte[] dstData = new byte[dstLength];
            buf.get(dstData, 0, dstLength);
            p.dst = InetAddress.getByAddress(dstData);
        }
        int dataLength = buf.getInt();
        p.data = new byte[dataLength];
        buf.get(p.data, 0, dataLength);
        return p;
    }
    private static byte[] marshal(InetAddress src, InetAddress dst, byte[] data) {
        byte[] srcData = src != null ? src.getAddress() : new byte[0];
        byte[] dstData = dst != null ? dst.getAddress() : new byte[0];
        return ByteBuffer.allocate(12 + srcData.length + dstData.length + data.length).putInt(srcData.length).put(srcData).putInt(dstData.length).put(dstData).putInt(data.length).put(data).array();
    }
    private static class Packet {
        private InetAddress src;
        private InetAddress dst;
        private byte[] data;
    }
}
