package utwente.ns.chatlayer;

import utwente.ns.chatlayer.protocol.ChatMessage;

import java.security.PrivateKey;

/**
 * Created by Harindu Perera on 4/14/17.
 */
public class DirectConversation extends ChatConversation {

    private final ChatClient.PeerInfo recipient;

    public DirectConversation(ChatClient client, ChatClient.PeerInfo recipient, PrivateKey signingKey) {
        super(client, ConversationType.DIRECT, recipient.peerSharedKey, signingKey);
        this.recipient = recipient;
    }

    @Override
    public void onNewMessage(ChatMessage message) {
        this.decryptAndAddMessage(this.recipient.publicKey, message);
        System.out.println(recipient.name + ": " + (message.getContent() == null ? "NULL (Error?)" : message.getContent().toString()));
    }

    @Override
    protected ChatClient.PeerInfo[] getParticipantInfo() {
        return new ChatClient.PeerInfo[]{recipient};
    }

}
