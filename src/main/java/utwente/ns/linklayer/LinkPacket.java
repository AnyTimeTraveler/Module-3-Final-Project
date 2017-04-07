package utwente.ns.linklayer;

import utwente.ns.IPacket;

/**
 * Created by simon on 07.04.17.
 */
public class LinkPacket implements IPacket{

    @Override
    public byte[] getData() {
        return new byte[0];
    }
}
