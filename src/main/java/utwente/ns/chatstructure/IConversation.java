package utwente.ns.chatstructure;

/**
 * Created by simon on 14.04.17.
 */
public interface IConversation {
    void sendMessage(String message);

    String getName();

    IUser[] getParticipants();

    IMessage[] getChatHistory();
}
