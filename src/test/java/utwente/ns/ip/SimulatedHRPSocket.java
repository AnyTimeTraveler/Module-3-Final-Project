package utwente.ns.ip;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.Util;
import utwente.ns.config.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Niels Overkamp on 13-Apr-17.
 */
public class SimulatedHRPSocket implements IHRP4Socket {
    public List<IReceiveListener> listeners = new ArrayList<>();
    private final SimulatedHRP4Layer simulatedHRP4Layer;
    public int dstPort;


    public SimulatedHRPSocket(SimulatedHRP4Layer simulatedHRP4Layer, int port) {
        this.simulatedHRP4Layer = simulatedHRP4Layer;
        this.dstPort = port;
    }

    @Override
    public void send(byte[] data, int dstAddress, int dstPort) throws IOException {
        HRP4Packet hrp4Packet = new HRP4Packet(
                Util.addressStringToInt(""),
                Util.addressStringToInt(""),
                (short) this.dstPort,
                (short) dstPort,
                Config.getInstance().defaultHRP4TTL,
                data
        );
        simulatedHRP4Layer.send(hrp4Packet);
    }

    @Override
    public synchronized void addReceiveListener(IReceiveListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void removeReceiveListener(IReceiveListener listener) {
        listeners.remove(listener);
    }

    @Override
    public int getDstPort() {
        return dstPort;
    }

    @Override
    public void close() throws IOException {
        this.simulatedHRP4Layer.close(this);
    }

    @Override
    public void receive(IPacket packet) {
        if (!(packet instanceof HRP4Packet)) {
            System.err.print("Got wrong packet: " + packet);
            return;
        }

        if (((int) ((HRP4Packet) packet).getDstPort() & 0xFFFF) != this.dstPort) {
            return;
        }

        listeners.parallelStream().forEach(listener -> listener.receive(packet));
    }
}
