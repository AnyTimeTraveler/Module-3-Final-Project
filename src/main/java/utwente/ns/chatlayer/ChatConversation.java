package utwente.ns.chatlayer;

import lombok.Getter;
import lombok.extern.java.Log;
import utwente.ns.chatlayer.exceptions.InvalidMessageException;
import utwente.ns.chatlayer.exceptions.UnsupportedMessageTypeException;
import utwente.ns.chatlayer.protocol.ChatMessage;
import utwente.ns.chatstructure.IConversation;
import utwente.ns.chatstructure.IMessage;
import utwente.ns.chatstructure.IUser;

import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Created by Harindu Perera on 4/14/17.
 */
@Log
public abstract class ChatConversation implements Comparable<ChatConversation>, IConversation {

    @Getter
    protected final ConversationType type;
    @Getter
    protected final ChatClient client;
    @Getter
    protected Key encryptionKey;
    @Getter
    protected PrivateKey signingKey;
    @Getter
    protected List<ChatMessage> messages = new LinkedList<>();
    @Getter
    protected long lastUpdated;
    protected Map<String, Integer> messageIdSet = new ConcurrentHashMap<>();

    /**
     * Constructs a new conversation.
     *
     * @param client        the ChatClient managing the conversation
     * @param type          the type of the conversation
     * @param encryptionKey the symmetric encryption key for messages
     * @param signingKey    the private key to sign messages
     */
    ChatConversation(ChatClient client, ConversationType type, Key encryptionKey, PrivateKey signingKey) {
        this.client = client;
        this.type = type;
        this.encryptionKey = encryptionKey;
        this.signingKey = signingKey;
    }

    /**
     * Adds a message to the conversation in a thread-safe way.
     * Messages with a duplicated ID will be dropped.
     *
     * @param message the message
     */
    private void addMessage(ChatMessage message) {
        synchronized (this.messages) {
            if (!this.messageIdSet.containsKey(message.getMessageId()))
                this.messages.add(message);
            else
                log.log(Level.WARNING, "Duplicate message ID \"" + message.getMessageId() + "\"for conversation detected; duplicate dropped");
        }
    }

    /**
     * Sends a message and adds message to conversation.
     *
     * @param message the message
     */
    public void sendMessage(ChatMessage message) {
        message.sign(this.signingKey);
        message.encryptContent(encryptionKey);
        try {
            this.client.sendChatMessage(message);
            message.setSent(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.addMessage(message);
    }

    /**
     * Processes a new incoming message
     *
     * @param message the message
     */
    public abstract void onNewMessage(ChatMessage message);

    /**
     * Decrypts, verifies, and adds a new incoming message.
     *
     * @param signingKey the public key of the sender
     * @param message    the message
     */
    protected void decryptAndAddMessage(PublicKey signingKey, ChatMessage message) {
        if (!message.verify(signingKey)) {
            // TODO: return here; preserved for debugging
            log.log(Level.WARNING, "Message verification failed");
        }

        try {
            message.decryptContent(this.encryptionKey);
        } catch (InvalidMessageException e) {
            log.log(Level.WARNING, "Invalid message received (InvalidMessageException at decryption); dropped");
            return;
        } catch (UnsupportedMessageTypeException e) {
            log.log(Level.WARNING, "Unsupported message type received (UnsupportedMessageTypeException at decryption); dropped");
            return;
        }

        this.addMessage(message);
    }

    /**
     * Gets the information about the participants in the chat excluding self.
     *
     * @return an participants
     */
    protected abstract ChatClient.PeerInfo[] getParticipantInfo();

    @Override
    public IUser[] getParticipants() {
        return this.getParticipantInfo();
    }

    @Override
    public IMessage[] getChatHistory() {
        return this.getMessages().toArray(new ChatMessage[this.messages.size()]);
    }

    @Override
    public int compareTo(ChatConversation o) {
        return o.lastUpdated > this.lastUpdated ? 1 : -1;
    }

    /**
     * Type of a chat
     */
    protected enum ConversationType {
        GROUP, DIRECT
    }
}
