package utwente.ns.chatstructure;

/**
 * Created by simon on 14.04.17.
 */
public interface IMessage {
    IUser getSender();
    IUser getReceiver();
    String getMessage();

}
