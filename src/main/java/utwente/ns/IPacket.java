package utwente.ns;

/**
 * Created by simon on 07.04.17.
 */
public interface IPacket {
    byte[] marshal();
    byte[] getData();
    String getIdent();

    default boolean isValid(byte[] data) {
        if (data.length < getIdent().getBytes().length) {
            return false;
        }

        for (int i = 0; i < getIdent().getBytes().length; i++) {
            if (getIdent().getBytes()[i] != data[i]) {
                return false;
            }
        }
        return true;
    }
}
