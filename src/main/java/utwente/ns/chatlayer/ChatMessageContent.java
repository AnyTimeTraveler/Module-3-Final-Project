package utwente.ns.chatlayer;

import lombok.extern.java.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Created by Harindu Perera on 4/11/17.
 */
@Log
public abstract class ChatMessageContent {

    public abstract void setContent(Key key, String encData, byte[] encIV);

    public static byte[] getDecryptedData(Key key, String encData, byte[] ivBytes) throws IllegalArgumentException {
        try {

            byte[] messageBytes = Base64.getDecoder().decode(encData);

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            aesCipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            return aesCipher.doFinal(messageBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
            return null;
        }
    }

    public String getEncryptedContent(Key key, byte[] ivBytes) {
        try {

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            aesCipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] encryptedBytes = aesCipher.doFinal(this.getData());

            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
            return null;
        }

    }

    public abstract byte[] getData();

    public abstract String getType();

}
