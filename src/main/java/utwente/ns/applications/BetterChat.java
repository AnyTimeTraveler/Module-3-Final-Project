package utwente.ns.applications;

import utwente.ns.NetworkStack;
import utwente.ns.chatlayer.ChatClient;
import utwente.ns.config.Config;

import java.io.IOException;

/**
 * Created by Harindu Perera on 4/14/17.
 */
public class BetterChat implements IApplication {

    @Override
    public void start() {
        try {
            NetworkStack networkStack = new NetworkStack();
            ChatClient chatClient = new ChatClient(Config.getInstance().name, networkStack);
            Thread chatThread = new Thread(chatClient::run);
            chatThread.setDaemon(false);
            chatThread.setName("ChatThreat");
            chatThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
