package utwente.ns.ip;

import lombok.Data;
import lombok.Getter;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.Util;
import utwente.ns.config.Config;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rhbvkleef
 *         Created on 4/10/17
 */
@Data
public class HRP4Socket implements IHRP4Socket, IReceiveListener, Closeable {
    /**
     * All listeners that shall be called upon receiving data
     */
    public List<IReceiveListener> listeners = new ArrayList<>();

    /**
     * The local port of this socket.
     */
    @Getter
    public int dstPort;

    /**
     * The underlying IP layer that shall be used for the actual transmission of data
     */
    private HRP4Layer ipLayer;

    /**
     * Deliberately package-private. In order to construct, call {@link HRP4Layer#open(int)}.
     *
     * @param ipLayer the underlying IP layer that is responsible for sending/receiving raw data
     * @param dstPort the local listener port
     * @see HRP4Layer
     */
    HRP4Socket(HRP4Layer ipLayer, int dstPort) {
        this.dstPort = dstPort;
        this.ipLayer = ipLayer;
    }

    /**
     * Receive is usually called by a {@link IHRP4Layer}. This is done upon receiving a packet.
     *
     * @param packet the packet that was received.
     */
    @Override
    public void receive(IPacket packet) {
        if (!(packet instanceof HRP4Packet)) {
            return;
        }

        if (((HRP4Packet) packet).getDstPort() != this.dstPort) {
            return;
        }

        listeners.parallelStream().forEach(listener -> listener.receive(packet));
    }

    /**
     * Send sends the data provided to the given dstAddress to the given port.
     *
     * @param data       The data to be sent
     * @param dstAddress The destination host
     * @param dstPort    The destination port
     * @throws IOException when sending has failed (this does not include packet loss)
     */
    public void send(byte[] data, int dstAddress, int dstPort) throws IOException {
        HRP4Packet hrp4Packet = new HRP4Packet(
                Util.addressToInt(InetAddress.getByName(Config.getInstance().myAddress)),
                dstAddress,
                (short) this.dstPort,
                (short) dstPort,
                Config.getInstance().defaultHRP4TTL,
                data
        );
        ipLayer.send(hrp4Packet);
    }

    /**
     * Add a listener to listen for incoming packets
     *
     * @param listener is called upon receiving incoming packets
     */
    public void addReceiveListener(IReceiveListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the provided listener
     *
     * @param listener removes the receiveListener
     */
    public void removeReceiveListener(IReceiveListener listener) {
        listeners.remove(listener);
    }

    /**
     * Closes the socket
     */
    @Override
    public void close() {
        this.ipLayer.close(this);
    }
}
