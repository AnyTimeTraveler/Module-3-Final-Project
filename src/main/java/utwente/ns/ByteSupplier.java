package utwente.ns;

/**
 * ByteSupplier is a interface used for creating functional byte supplier functions.
 * @author rhbvkleef
 *         Created on 4/20/17
 */
@FunctionalInterface
public interface ByteSupplier {
    byte getAsByte();
}
