package utwente.ns.chatlayer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import utwente.ns.chatstructure.IUser;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by Harindu Perera on 4/12/17.
 */
@NoArgsConstructor
public class PeerIdentity implements Comparable, IUser {

    public String id;
    public String name;
    public String address;
    public String publicKey;

    public transient Date updateTime = new Date();
    public transient String fingerprint;

    public PeerIdentity(String id, String name, String address, String publicKey) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.publicKey = publicKey;
    }

    public String getFingerprint() {

        if (fingerprint != null) return fingerprint;

        byte[] hash = CryptoUtil.getSHA256Hash((id+name+address+publicKey).getBytes());

        String id = String.format("%02x", hash[0]);

        for (int i = 1; i < hash.length; i++)
            id += String.format("%02x", hash[i]);

        return id;
    }

    public int compareTo(Object obj) {
        if (obj instanceof PeerIdentity && ((PeerIdentity) obj).updateTime.after(this.updateTime)) return 1;
        return -1;
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof PeerIdentity && ((PeerIdentity) obj).getFingerprint().equals(this.getFingerprint()));
    }

    @Override
    public int hashCode() {
        return this.getFingerprint().hashCode();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUniqueID() {
        return this.id;
    }

    @Override
    public boolean isConfirmed() {
        return false;
    }

    @Override
    public String toString() {
        return "ID: " + id + "\n" +
                "Name: " + name + "\n" +
                "Address: " + address + "\n" +
                "Fingerprint: " + getFingerprint();
    }
}
