package utwente.ns.linklayer;

import utwente.ns.IReceiveListener;
import utwente.ns.ip.HIP4Packet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by simon on 07.04.17.
 */
public class LinkLayer {

    private List<IReceiveListener> packetListeners;

    public LinkLayer() {
        packetListeners = new ArrayList<>();
    }

    public void send(HIP4Packet packet) {

    }

    public void addReceiveListener(IReceiveListener receiver) {

    }

    public int getLinkCost(String address){
        return 1;
    }
}
