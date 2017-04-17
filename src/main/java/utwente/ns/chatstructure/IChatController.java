package utwente.ns.chatstructure;

/**
 * Created by simon on 14.04.17.
 */
public interface IChatController {
    IConversation[] getConversations();

    void addConversation(String name, IUser... users);

    IUser[] getNewUsers();

    IUser[] getConnectedUsers();

    IUser getUserById(String id);

    void sendMessage(IConversation conversation, String message);
}
