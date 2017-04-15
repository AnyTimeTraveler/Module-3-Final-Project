package utwente.ns.ip;

import lombok.Data;
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
    public List<IReceiveListener> listeners = new ArrayList<>();
    public short dstPort;
    private HRP4Layer ipLayer;

    /**
     * Deliberately package-private. In order to construct, call {@link HRP4Layer#open(short)}.
     *
     * @param ipLayer the underlying IP layer that is responsible for sending/receiving raw data
     * @param dstPort the local listener port
     *
     * @see HRP4Layer
     */
    HRP4Socket(HRP4Layer ipLayer, short dstPort) {
        this.dstPort = dstPort;
        this.ipLayer = ipLayer;
    }

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

    public void send(byte[] data, int dstAddress, short dstPort) throws IOException {
        HRP4Packet hrp4Packet = new HRP4Packet(
                Util.addressToInt(InetAddress.getByName(Config.getInstance().getMyAddress())),
                dstAddress,
                this.dstPort,
                dstPort,
                Config.getInstance().getDefaultHRP4TTL(),
                data
        );
        ipLayer.send(hrp4Packet);
    }

    public void addReceiveListener(IReceiveListener listener) {
        listeners.add(listener);
    }

    public void removeReceiveListener(IReceiveListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        this.ipLayer.close(this);
    }
}
