package utwente.ns.linklayer;

import lombok.Data;
import utwente.ns.IPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Created by simon on 07.04.17.
 */
@Data
public class LinkPacket implements IPacket {

    private final byte[] data;
    private final InetAddress receivedPacketAddress;

    public LinkPacket(DatagramPacket receivedPacket) {
        data = receivedPacket.getData();
        receivedPacketAddress = receivedPacket.getAddress();
    }

    @Override
    public byte[] marshal() {
        return new byte[0];
    }

    @Override
    public String getIdent() {
        return "LNK0";
    }
}
