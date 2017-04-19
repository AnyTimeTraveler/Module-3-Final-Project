package utwente.ns.chatlayer.network;

/**
 * Created by Harindu Perera on 4/16/17.
 */
public interface IRequestHandler {
    byte[] handleData(String addr, int port, byte[] data);
}
