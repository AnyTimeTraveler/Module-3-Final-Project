package utwente.ns.ip;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.tcp.RTP4Packet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Niels Overkamp on 13-Apr-17.
 */
public class SimulatedHRP4Layer implements IHRP4Layer {

    private List<IReceiveListener> receiveListeners;

    public SimulatedHRP4Layer() {
        this.receiveListeners = new ArrayList<>();
    }

    @Override
    public void receive(IPacket packet) {
        receiveListeners.forEach(iReceiveListener -> {if (iReceiveListener != null){iReceiveListener.receive(packet);}});
    }

    @Override
    public void send(IPacket packet) throws IOException {
        RTP4Packet rtp4Packet;
        try {
            rtp4Packet = new RTP4Packet(packet.getData());
        } catch (PacketMalformedException e) {
            return;
        }
        System.out.print(Thread.currentThread().getName() + "> ");
        if (packet instanceof HRP4Packet){
            HRP4Packet hrp4Packet = ((HRP4Packet) packet);
            System.out.print(hrp4Packet.getSrcPort() + "->" + hrp4Packet.getDstPort() + " ");
        } else {
            System.out.print("? ");
        }
        System.out.println("sent: " + rtp4Packet.toString());
        receive(packet);
    }

    @Override
    public void addReceiveListener(IReceiveListener receiver) {
        this.receiveListeners.add(receiver);
    }

    @Override
    public SimulatedHRPSocket open(short port) throws IOException {
        if (this.receiveListeners.parallelStream()
                .filter(listener -> listener instanceof HRP4Socket)
                .map(listener -> (HRP4Socket) listener)
                .anyMatch(listener -> listener.getDstPort() == port)) {
            throw new IOException("Port already opened");
        }
        SimulatedHRPSocket simulatedHRPSocket = new SimulatedHRPSocket(this, port);
        this.addReceiveListener(simulatedHRPSocket);
        return simulatedHRPSocket;
    }

    @Override
    public HRP4Router getRouter() {
        return null;
    }

    void close(SimulatedHRPSocket simulatedHRPSocket) {
        receiveListeners.remove(simulatedHRPSocket);
    }
}
