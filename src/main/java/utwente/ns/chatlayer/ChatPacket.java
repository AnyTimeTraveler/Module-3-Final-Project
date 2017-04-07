package utwente.ns.chatlayer;

import utwente.ns.IPacket;

/**
 * Created by simon on 07.04.17.
 */
public class ChatPacket implements IPacket {
    @Override
    public byte[] getData() {
        return new byte[0];
    }
}
