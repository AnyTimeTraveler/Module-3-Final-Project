package utwente.ns.tcp;

import utwente.ns.IPacket;

/**
 * Created by simon on 07.04.17.
 */
public class TCPPacket implements IPacket {
    @Override
    public byte[] getData() {
        return new byte[0];
    }
}
