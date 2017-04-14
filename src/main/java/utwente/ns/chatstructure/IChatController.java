package utwente.ns.chatstructure;

/**
 * Created by simon on 14.04.17.
 */
public interface IChatController {
    IConversation[] getConversations();

    void addConversation(String name, IUser... users);

    IUser[] getNewUsers();

    IUser[] getComnnectedUsers();

    IUser getUserById(int id);

    void sendMessage(IUser user, String message);
}
