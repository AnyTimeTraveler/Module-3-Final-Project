package utwente.ns.chatlayer;

import lombok.extern.java.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Created by harindu on 4/11/17.
 */
@Log
public abstract class ChatMessageContent {

    public static final String KEY_SEP = ";";

    public ChatMessageContent(Key key, String data) {
        // do nothing;
    }

    public static byte[] getDecryptedData(PrivateKey key, String encData) throws IllegalArgumentException {
        try {

            String[] comps = encData.split(KEY_SEP);
            if (comps.length != 2) throw new IllegalArgumentException();

            byte[] encryptedKeyBytes = Base64.getDecoder().decode(comps[0]);
            byte[] messageBytes = Base64.getDecoder().decode(comps[1]);

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, key);

            byte[] messageKeyData = rsaCipher.doFinal(encryptedKeyBytes);

            Key messageKey = new SecretKeySpec(messageBytes, "AES");

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, messageKey);

            byte[] decryptedBytes = aesCipher.doFinal(messageBytes);

            return decryptedBytes;

        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
            return null;
        }
    }

    public String getEncryptedContent(PublicKey key) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key messageKey =  keyGenerator.generateKey();

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, messageKey);

            byte[] encryptedBytes = aesCipher.doFinal(this.getData());

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedKeyBytes = aesCipher.doFinal(messageKey.getEncoded());

            return Base64.getEncoder().encodeToString(encryptedKeyBytes) + KEY_SEP + Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
            return null;
        }

    }

    public abstract byte[] getData();

}
