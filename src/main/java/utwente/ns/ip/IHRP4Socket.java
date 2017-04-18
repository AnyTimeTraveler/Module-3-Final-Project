package utwente.ns.ip;

import utwente.ns.IReceiveListener;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Niels Overkamp on 13-Apr-17.
 */
public interface IHRP4Socket extends IReceiveListener, Closeable {

    void send(byte[] data, int dstAddress, int dstPort) throws IOException;
    void addReceiveListener(IReceiveListener listener);
    void removeReceiveListener(IReceiveListener listener);
    int getDstPort();
    
}
