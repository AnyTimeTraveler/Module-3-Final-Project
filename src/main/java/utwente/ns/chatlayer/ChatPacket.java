package utwente.ns.chatlayer;

import lombok.Getter;
import utwente.ns.IPacket;

/**
 * Created by simon on 07.04.17.
 */
public class ChatPacket implements IPacket {
    @Getter
    private byte[] data;

    @Override
    public byte[] marshall() {
        return new byte[0];
    }
}
