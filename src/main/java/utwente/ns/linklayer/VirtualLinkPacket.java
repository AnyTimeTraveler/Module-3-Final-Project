package utwente.ns.linklayer;

import lombok.Data;
import utwente.ns.IPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Created by simon on 07.04.17.
 */
@Data
public class VirtualLinkPacket implements IPacket {

    private final byte[] data;
    private final InetAddress receivedPacketAddress;

    VirtualLinkPacket(DatagramPacket receivedPacket) {
        data = new byte[receivedPacket.getLength()];
        System.arraycopy(receivedPacket.getData(),0,data,0,receivedPacket.getLength());
        receivedPacketAddress = receivedPacket.getAddress();
    }

    @Override
    public byte[] marshal() {
        return data;
    }
}
