package utwente.ns.tcp;

/**
 * Created by simon on 07.04.17.
 */
public class RTP4Connection {
    private final int address;
    private final int port;
    private final RTP4Socket socket;

    public RTP4Connection(int address, int port, RTP4Socket socket) {
        this.address = address;
        this.port = port;
        this.socket = socket;
    }

    public void send(byte[] data){
        socket.send(data);
    }
}
