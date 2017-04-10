package utwente.ns;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

    public static int addressStringToInt(String address) {
        // I'm too tired right now, to figure out what I have to implement here.
        throw new NotImplementedException();
    }
}
