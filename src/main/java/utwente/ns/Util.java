package utwente.ns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author rhbvkleef
 *         Created on 4/7/17
 */
public class Util {
    public static byte[] intToByteArr(int in) {
        return ByteBuffer.allocate(4).putInt(in).array();
    }

    public static byte[] shortToByteArr(short in) {
        return ByteBuffer.allocate(2).putShort(in).array();
    }

    public static int addressStringToInt(String address) throws UnknownHostException {
        return addressToInt(InetAddress.getByName(address));
    }
    
    public static int addressToInt(InetAddress localAddress) {
        return ByteBuffer.wrap(localAddress.getAddress()).getInt(0);
    }

    public static String figlet(String text) {
        try {
            ProcessBuilder builder = new ProcessBuilder("bash", "-c", text);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            StringBuilder b = new StringBuilder();
            while (true) {
                line = r.readLine();
                if (line == null) break;
                b.append(line);
                b.append("\n");
            }
            p.waitFor(1, TimeUnit.SECONDS);
            if (p.exitValue() != 0) {
                return text;
            }
            return b.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return text;
        }
    }
}
