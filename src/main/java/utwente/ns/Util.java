package utwente.ns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

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
}
