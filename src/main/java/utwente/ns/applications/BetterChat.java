package utwente.ns.applications;

import lombok.extern.java.Log;
import utwente.ns.NetworkStack;
import utwente.ns.chatlayer.ChatClient;
import utwente.ns.chatlayer.PeerIdentity;
import utwente.ns.chatstructure.IUser;
import utwente.ns.config.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Harindu Perera on 4/14/17.
 */
@Log
public class BetterChat implements IApplication {

    private static final String HELP = "Hi, this is the better chat (which is better than the other chat, duh).\n\n"
            + "Commands (colon not part of command):\n\n"
            + "available: see available peers to connect to (with fingerprints)\n\n"
            + "connected: see connected peers to chat with\n\n"
            + "add;<ID>: connect with an available peer\n\n"
            + "<ID>;<message>: send a message to a connected peer\n";

    private NetworkStack networkStack;
    private ChatClient chatClient;

    private void exec(String command) {
        String cmd = command.split(";")[0];
        switch (cmd) {
            case "available":
                IUser[] availableUsers = this.chatClient.getNewUsers();
                for (int i = 0; i < availableUsers.length; i++) System.out.println(availableUsers[i].toString());
                break;
            case "connected":
                IUser[] users = this.chatClient.getConnectedUsers();
                for (int i = 0; i < users.length; i++) System.out.println(users[i].toString());
                break;
            case "add":
                String userId;
                try {
                    userId = command.split(";")[1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Invalid command, moron!");
                    return;
                }
                boolean added = this.chatClient.addPeerById(userId);
                if (added) {
                    System.out.println("Peer connected");
                } else {
                    System.out.println("Peer addition failed");
                }
                break;
            default:
                String message;
                try {
                    message = command.split(";")[1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Invalid command, moron!");
                    return;
                }
                IUser user = chatClient.getUserById(cmd);
                if (user == null) {
                    System.out.println("User not connected");
                    return;
                }
                chatClient.sendMessage(user, message);
        }
    }

    @Override
    public void start() {
        try {
            networkStack  = new NetworkStack();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        chatClient = new ChatClient(Config.getInstance().getName(), networkStack);
        chatClient.run();
        System.out.println(HELP);
        new BufferedReader(new InputStreamReader(System.in)).lines().forEach(line -> {
            try {
                exec(line);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        });
    }

}
