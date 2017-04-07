package utwente.ns.linklayer;

import lombok.Getter;
import utwente.ns.IPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Created by simon on 07.04.17.
 */
public class LinkPacket implements IPacket{

    @Getter
    private final byte[] data;
    @Getter
    private final InetAddress receivedPacketAddress;

    public LinkPacket(DatagramPacket receivedPacket) {
        data = receivedPacket.getData();
        receivedPacketAddress = receivedPacket.getAddress();
    }

    @Override
    public byte[] marshall() {
        return new byte[0];
    }
}
