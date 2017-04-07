package utwente.ns;

import utwente.ns.linklayer.LinkPacket;

/**
 * Created by simon on 07.04.17.
 */
public interface IReceiveListener {
    void receive(IPacket packet);
}
