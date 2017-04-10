package utwente.ns.chatlayer;

/**
 * Created by harindu on 4/10/17.
 */
public class ChatMessage {
    private String senderId;
    private String messageId;
    private String groupId;
    private long timestamp;

    private String hash;

    public ChatMessage(String senderId, String messageId, String groupId, long timestamp) {
        this.senderId = senderId;
        this.messageId = messageId;
        this.groupId = groupId;
        this.timestamp = timestamp;
    }

}
