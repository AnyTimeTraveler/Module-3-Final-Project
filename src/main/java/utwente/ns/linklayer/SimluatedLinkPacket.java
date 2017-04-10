package utwente.ns.linklayer;

import lombok.Data;
import utwente.ns.IPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Created by simon on 07.04.17.
 */
@Data
public class SimluatedLinkPacket implements IPacket {

    private final byte[] data;
    private final InetAddress receivedPacketAddress;

    public SimluatedLinkPacket(DatagramPacket receivedPacket) {
        data = receivedPacket.getData();
        receivedPacketAddress = receivedPacket.getAddress();
    }

    @Override
    public byte[] marshal() {
        return new byte[0];
    }
}
