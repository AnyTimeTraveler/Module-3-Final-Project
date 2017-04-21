package utwente.ns.ip;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.PacketMalformedException;
import utwente.ns.Util;
import utwente.ns.tcp.RTP4Layer;
import utwente.ns.tcp.RTP4Packet;

import java.io.IOException;
import java.util.*;

/**
 * Created by Niels Overkamp on 13-Apr-17.
 */
public class SimulatedHRP4Layer implements IHRP4Layer {

    private List<IReceiveListener> receiveListeners;

	private NavigableSet<Integer> ports = new TreeSet<>();

    public SimulatedHRP4Layer() {
        this.receiveListeners = new ArrayList<>();
    }

    @Override
    public void receive(IPacket packet) {
        receiveListeners.forEach(iReceiveListener -> iReceiveListener.receive(packet));
    }

    @Override
    public void send(IPacket packet) throws IOException {
        RTP4Packet rtp4Packet;
        try {
            rtp4Packet = new RTP4Packet(packet.getData());
        } catch (PacketMalformedException e) {
            return;
        }
        if (new Random().nextInt(100) > 90) {
            if (RTP4Layer.DEBUG) System.out.println(Thread.currentThread().getName() + "> " + "Dropped packet " + rtp4Packet);
            return;
        }
        if (RTP4Layer.DEBUG) System.out.print(Thread.currentThread().getName() + "> ");
        if (packet instanceof HRP4Packet){
            HRP4Packet hrp4Packet = ((HRP4Packet) packet);
            if (RTP4Layer.DEBUG) System.out.print(hrp4Packet.getSrcPort() + "->" + hrp4Packet.getDstPort() + " ");
        } else {
            if (RTP4Layer.DEBUG) System.out.print("? ");
        }
        if (RTP4Layer.DEBUG) System.out.println("successful: " + rtp4Packet.toString());
        receive(packet);
    }

    @Override
    public synchronized void addReceiveListener(IReceiveListener receiver) {
        this.receiveListeners.add(receiver);
        if (receiver instanceof IHRP4Socket) {
        	ports.add((int) ((IHRP4Socket) receiver).getDstPort());
		}
    }

    @Override
    public SimulatedHRPSocket open(int port) throws IOException {
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
	public IHRP4Socket openRandom() throws IOException {
		return this.open(Util.randomNotInSet(ports, 1024, 2048));
	}

    @Override
    public HRP4Router getRouter() {
        return null;
    }

    void close(SimulatedHRPSocket simulatedHRPSocket) {
        receiveListeners.remove(simulatedHRPSocket);
        ports.remove((int) simulatedHRPSocket.getDstPort());
    }
}
