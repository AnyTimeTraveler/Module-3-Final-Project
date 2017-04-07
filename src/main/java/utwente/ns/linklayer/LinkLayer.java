package utwente.ns.linklayer;

import utwente.ns.IReceiveListener;

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

    public void send() {

    }

    public void addReceiveListener(IReceiveListener receiver) {

    }

    public int getLinkCost(String address){
        return 1;
    }
}
