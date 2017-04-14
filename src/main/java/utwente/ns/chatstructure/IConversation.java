package utwente.ns.chatstructure;

/**
 * Created by simon on 14.04.17.
 */
public interface IConversation {
    IUser[] getParticipants();
    IMessage[] getChatHistory();
}
