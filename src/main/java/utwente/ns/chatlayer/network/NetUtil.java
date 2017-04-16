package utwente.ns.chatlayer.network;

import utwente.ns.NetworkStack;
import utwente.ns.Util;
import utwente.ns.tcp.RTP4Connection;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Created by Harindu Perera on 4/17/17.
 */
public class NetUtil {

    public static <T> T doRTP4JsonRequest(Object request, String address, int port, NetworkStack networkStack, Class<T> responseClass) throws IOException, InterruptedException {
        // TODO: address the whole closing issue
        byte[] requestData = Util.toJsonBytes(request);
        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
        try (RTP4Connection connection = networkStack.getRtp4Layer().connect(address, port)) {
            connection.send(requestData);
            while (!connection.isClosed()) {
                responseBuffer.write(connection.receive());
            }
            responseBuffer.close();
        }
        if (responseBuffer.size() == 0) throw new EOFException();
        return Util.fromJsonBytes(responseBuffer.toByteArray(), responseClass);
    }

}
