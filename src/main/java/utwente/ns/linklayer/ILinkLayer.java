package utwente.ns.linklayer;

import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by simon on 10.04.17.
 */
public interface ILinkLayer extends Closeable {
    void send(IPacket packet) throws IOException;
    
    void send(byte[] data) throws IOException;
    
    void addReceiveListener(IReceiveListener receiver);
    
    void removeReceiveListener(IReceiveListener receiver);

    @Override
    void close() throws IOException;
}
