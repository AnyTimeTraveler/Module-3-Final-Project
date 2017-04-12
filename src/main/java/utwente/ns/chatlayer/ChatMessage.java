package utwente.ns.chatlayer;

import lombok.Getter;
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

    public transient static final String CONTENT_TYPE_TEXT = "text";
    public transient static final String CONTENT_TYPE_IMAGE = "image";

    @Getter
    private String senderId;
    @Getter
    private String messageId;
    @Getter
    private String recipientId;
    @Getter
    private String groupId;
    @Getter
    private long timestamp;
    @Getter
    private String type;
    @Getter
    private String data;
    private String signature;

    private transient ChatMessageContent content;

    public ChatMessage(String senderId, String messageId, String recipientId, String groupId, String type) {
        this.senderId = senderId;
        this.messageId = messageId;
        this.recipientId = recipientId;
        this.groupId = groupId;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    public ChatMessage(String senderId, String messageId, String recipientId, String groupId, String type, String text) {
        this(senderId, messageId, recipientId, groupId, type);
        this.content = new TextMessageContent(text);
    }

    public ChatMessage() {
        // do nothing
    }

    public void encryptContent(Key key) {
        this.data = this.content.getEncryptedContent(key);
    }

    public void decryptContent(Key key) throws InvalidMessageException, UnsupportedMessageTypeException {
        ChatMessageContent content;
        switch (this.type) {
            case CONTENT_TYPE_TEXT:
                this.content = new TextMessageContent();
                this.content.setContent(key, this.data);
            case CONTENT_TYPE_IMAGE:
                throw new UnsupportedMessageTypeException();
            default:
                throw new InvalidMessageException();
        }
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
        signature.update(data.getBytes());
    }

}
