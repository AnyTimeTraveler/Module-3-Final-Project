package utwente.ns.chatlayer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import utwente.ns.chatstructure.IMessage;
import utwente.ns.chatstructure.IUser;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Created by Harindu Perera on 4/10/17.
 */
@Log
public class ChatMessage implements IMessage {

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
    private long sendTime;
    @Getter
    @Setter
    private transient long receiveTime;
    @Getter
    @Setter
    private transient boolean sent = false;
    @Getter
    private String type;
    @Getter
    private String data;
    private byte[] encIV;
    private String signature;

    @Getter
    private transient ChatMessageContent content;

    public ChatMessage(String senderId, String messageId, String recipientId, String groupId, String type) {
        this.senderId = senderId;
        this.messageId = messageId;
        this.recipientId = recipientId;
        this.groupId = groupId;
        this.sendTime = System.currentTimeMillis();
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
        SecureRandom randomSecureRandom = null;
        try {
            randomSecureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.encIV  = new byte[16];
        randomSecureRandom.nextBytes(encIV);
        this.data = this.content.getEncryptedContent(key, encIV);
    }

    public void decryptContent(Key key) throws InvalidMessageException, UnsupportedMessageTypeException {
        ChatMessageContent content;
        switch (this.type) {
            case CONTENT_TYPE_TEXT:
                this.content = new TextMessageContent();
                this.content.setContent(key, this.data, this.encIV);
            case CONTENT_TYPE_IMAGE:
                throw new UnsupportedMessageTypeException();
            default:
                throw new InvalidMessageException();
        }
    }

    public void sign(PrivateKey key) {
        try {
            Signature signature = Signature.getInstance("SHA1withECDSA");
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
            Signature signature = Signature.getInstance("SHA1withECDSA");
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
        if (groupId != null) signature.update(groupId.getBytes());
        signature.update(ByteBuffer.allocate(Long.BYTES).putLong(sendTime).array());
        signature.update(type.getBytes());
        signature.update(data.getBytes());
    }

    @Override
    public String getSender() {
        return this.getSenderId();
    }

    @Override
    public String getReceiver() {
        return this.getRecipientId();
    }

    @Override
    public String getMessage() {
        if (this.content instanceof TextMessageContent) {
            return ((TextMessageContent) this.content).text;
        }
        return null;
    }

    @Override
    public long getTimeOfSending() {
        return this.getSendTime();
    }

    @Override
    public long getTimeOfReceiving() {
        return this.getReceiveTime();
    }
}
