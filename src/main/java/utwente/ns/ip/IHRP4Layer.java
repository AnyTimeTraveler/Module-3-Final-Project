package utwente.ns.ip;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;

import java.io.IOException;

/**
 * Created by Niels Overkamp on 13-Apr-17.
 */
public interface IHRP4Layer extends IReceiveListener {

    void send(IPacket packet) throws IOException;
    void addReceiveListener(IReceiveListener receiver);
    IHRP4Socket open(short port) throws IOException;
    HRP4Router getRouter();


}
