package utwente.ns.ip;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
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
    public short dstPort;


    public SimulatedHRPSocket(SimulatedHRP4Layer simulatedHRP4Layer, short port) {
        this.simulatedHRP4Layer = simulatedHRP4Layer;
        this.dstPort = port;
    }

    @Override
    public void send(byte[] data, int dstAddress, short dstPort) throws IOException {
        HRP4Packet hrp4Packet = new HRP4Packet(
                0,
                0,
                this.dstPort,
                dstPort,
                Config.getInstance().getDefaultHRP4TTL(),
                data
        );
        simulatedHRP4Layer.send(hrp4Packet);
    }

    @Override
    public void addReceiveListener(IReceiveListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeReceiveListener(IReceiveListener listener) {
        listeners.remove(listener);
    }

    @Override
    public short getDstPort() {
        return dstPort;
    }

    @Override
    public void close() throws IOException {
        this.simulatedHRP4Layer.close(this);
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
}
