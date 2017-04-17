package utwente.ns;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.NavigableSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author rhbvkleef
 *         Created on 4/7/17
 */
public class Util {

    private static Gson gson = new Gson();

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

    public static String intToAddressString(int integer) throws UnknownHostException {
        return intToAddress(integer).getHostAddress();
    }

    public static InetAddress intToAddress(int integer) throws UnknownHostException {
        return InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(integer).array());
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

    public static String toJsonString(Object src) {
        return gson.toJson(src);
    }

    public static byte[] toJsonBytes(Object src) {
        return toJsonString(src).getBytes();
    }

    public static <T> T fromJsonString(String str, Class<T> classOfT) {
        return gson.fromJson(str, classOfT);
    }

    public static <T> T fromJsonBytes(byte[] bytes, Class<T> classOfT) {
        return gson.fromJson(new String(bytes), classOfT);
    }

    public static int randomNotInSet(NavigableSet<Integer> set, int start, int stop) {
        int value = start + new Random().nextInt(stop - start);

        for (Integer i : set) {
            if (i >= start && i <= value) value++;
        }

        return value;
    }
}
