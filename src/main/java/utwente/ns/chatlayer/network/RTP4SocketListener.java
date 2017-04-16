package utwente.ns.chatlayer.network;

import lombok.AllArgsConstructor;
import utwente.ns.tcp.RTP4Connection;
import utwente.ns.tcp.RTP4Socket;

import java.io.IOException;

/**
 * Created by Harindu Perera on 4/17/17.
 */

@AllArgsConstructor
public class RTP4SocketListener implements Runnable {

    RTP4Socket socket;
    IRequestHandler requestHandler;

    @Override
    public void run() {
        while (!this.socket.isClosed()) {
            try {
                RTP4Connection connection = socket.accept();
                RTP4ConnectionHandler.handleConnection(connection, this.requestHandler);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

}
