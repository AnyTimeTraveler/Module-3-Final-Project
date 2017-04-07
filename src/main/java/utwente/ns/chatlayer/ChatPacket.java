package utwente.ns.chatlayer;

import lombok.Data;
import utwente.ns.IPacket;

/**
 * Created by simon on 07.04.17.
 */
@Data
public class ChatPacket implements IPacket {
    private byte[] data;

    @Override
    public byte[] marshal() {
        return new byte[0];
    }
}
