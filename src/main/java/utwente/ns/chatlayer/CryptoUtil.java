package utwente.ns.chatlayer;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Created by harindu on 4/12/17.
 */
public class CryptoUtil {

    public static final int PBKDF2_ITERATIONS = 1000;

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey decodePublicKey(String keyData) throws InvalidKeySpecException {
        byte[] keyDataBytes = Base64.getDecoder().decode(keyData);

        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyDataBytes);
        return kf.generatePublic(keySpec);
    }

    public static Key generateSharedKey(PrivateKey ownPrivateKey, PublicKey peerPublicKey) throws InvalidKeyException {

        KeyAgreement ka = null;
        try {
            ka = KeyAgreement.getInstance("ECDH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        ka.init(ownPrivateKey);
        ka.doPhase(peerPublicKey, true);

        PBEKeySpec spec = new PBEKeySpec(new String(ka.generateSecret()).toCharArray(), new byte[0], PBKDF2_ITERATIONS, 128);
        SecretKeyFactory skf = null;
        try {
            skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        byte[] derivedKeyBytes;
        try {
            derivedKeyBytes = skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }

        SecretKeySpec secretKeySpec = new SecretKeySpec(derivedKeyBytes, "AES");

        return secretKeySpec;
    }

    public static byte[] getSHA256Hash(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
