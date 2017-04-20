package utwente.ns.chatlayer.network;

import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import utwente.ns.tcp.RTP4Connection;
import utwente.ns.tcp.RTP4Socket;

import java.io.IOException;
import java.sql.Time;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Created by Harindu Perera on 4/17/17.
 */

@AllArgsConstructor
@Log
public class RTP4SocketListener implements Runnable {

    RTP4Socket socket;
    IRequestHandler requestHandler;

    @Override
    public void run() {
        while (true) {
            try {
                RTP4Connection connection = socket.accept();
                RTP4ConnectionHandler.handleConnection(connection, this.requestHandler);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } catch (TimeoutException e) {
                log.log(Level.INFO, "Connection establishment timed out");
            }
        }
    }

}
