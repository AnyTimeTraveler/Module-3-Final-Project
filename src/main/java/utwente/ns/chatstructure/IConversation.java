package utwente.ns.chatstructure;

import utwente.ns.chatlayer.ChatMessage;

/**
 * Created by simon on 14.04.17.
 */
public interface IConversation {
    void sendMessage(ChatMessage message);

    IUser[] getParticipants();
    IMessage[] getChatHistory();
}
