package utwente.ns.tcp;

import lombok.Data;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by simon on 07.04.17.
 */
@Data
public class RTP4Connection implements Closeable{
    private final int address;
    private final int port;
    private final RTP4Socket socket;

    private BlockingQueue<byte[]> dataQueue;

    RTP4Connection(int address, int port, RTP4Socket socket) {
        this.address = address;
        this.port = port;
        this.socket = socket;
        this.dataQueue = new LinkedBlockingQueue<>();
    }

    public void send(byte[] data){
        socket.send(data, address, (short) port);
    }

    public byte[] receive() throws InterruptedException {
        return dataQueue.take();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public boolean isClosed(){
        return socket.isClosed();
    }


    public void receiveData(byte[] data) {
        dataQueue.add(data);
    }
}
