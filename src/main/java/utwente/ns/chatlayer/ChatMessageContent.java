package utwente.ns.chatlayer;

import lombok.extern.java.Log;

import javax.crypto.Cipher;
import java.security.Key;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Created by harindu on 4/11/17.
 */
@Log
public abstract class ChatMessageContent {

    public abstract void setContent(Key key, String encData);

    public static byte[] getDecryptedData(Key key, String encData) throws IllegalArgumentException {
        try {

            byte[] messageBytes = Base64.getDecoder().decode(encData);

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, key);

            return aesCipher.doFinal(messageBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
            return null;
        }
    }

    public String getEncryptedContent(Key key) {
        try {

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedBytes = aesCipher.doFinal(this.getData());

            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
            return null;
        }

    }

    public abstract byte[] getData();

}
