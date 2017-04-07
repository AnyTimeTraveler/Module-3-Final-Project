package utwente.ns;

/**
 * Created by simon on 07.04.17.
 */
@FunctionalInterface
public interface IReceiveListener {
    void receive(IPacket packet);
}
