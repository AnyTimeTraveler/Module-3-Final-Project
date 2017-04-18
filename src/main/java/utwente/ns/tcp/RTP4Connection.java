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

    BlockingQueue<byte[]> receivedDataQueue;
    BlockingQueue<byte[]> sendDataQueue;

    RTP4Connection(int address, int port, RTP4Socket socket) {
        this.address = address;
        this.port = port;
        this.socket = socket;
        this.receivedDataQueue = new LinkedBlockingQueue<>();
        this.sendDataQueue = new LinkedBlockingQueue<>();
    }

    public void send(byte[] data){
        sendDataQueue.add(data);
    }

    public byte[] receive() throws InterruptedException {
        return receivedDataQueue.take();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public boolean isClosed(){
        return socket.isClosed();
    }

    public boolean canSend() {
        return (!isClosed()) || socket.unacknowledgedQueue.size() < 1;
    }

    public boolean isDone() {
        return socket.unacknowledgedQueue.isEmpty() && sendDataQueue.isEmpty();
    }
    void receiveData(byte[] data) {
        receivedDataQueue.add(data);
    }
}
