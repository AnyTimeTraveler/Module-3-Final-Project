package utwente.ns.chatlayer;

import lombok.extern.java.Log;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Created by harindu on 4/10/17.
 */
@Log
public class ChatMessage {
    private String senderId;
    private String messageId;
    private String recipientId;
    private String groupId;
    private long timestamp;
    private String type;
    private String content;
    private String signature;

    public ChatMessage(String senderId, String messageId, String groupId, String type) {
        this.senderId = senderId;
        this.messageId = messageId;
        this.groupId = groupId;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    public ChatMessage() {
        // do nothing
    }

    public void sign(PrivateKey key) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            appendSigData(signature);
            this.signature = Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    public boolean verify(PublicKey key) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            appendSigData(signature);
            return signature.verify(Base64.getDecoder().decode(this.signature));
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }

    private void appendSigData(Signature signature) throws SignatureException {
        signature.update(senderId.getBytes());
        signature.update(messageId.getBytes());
        signature.update(recipientId.getBytes());
        signature.update(groupId.getBytes());
        signature.update(ByteBuffer.allocate(Long.BYTES).putLong(timestamp).array());
        signature.update(type.getBytes());
        signature.update(content.getBytes());
    }

}
