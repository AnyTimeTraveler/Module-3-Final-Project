package utwente.ns.chatstructure;

/**
 * Created by simon on 14.04.17.
 */
public interface IMessage {
    String getSender();
    String getReceiver();
    String getMessage();
    long getTimeOfSending();
    long getTimeOfReceiving();
}
