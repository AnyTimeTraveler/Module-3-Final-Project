package utwente.ns.ui;

import utwente.ns.chatlayer.ChatClient;
import utwente.ns.chatstructure.IUser;
import utwente.ns.chatstructure.IUserInterface;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by simon on 15.04.17.
 */
public class SimpleTUI implements IUserInterface {

    private static final String HELP = "Hi, this is the better chat (which is better than the other chat, duh).\n\n" + "Commands (colon not part of command):\n\n" + "available: see available peers to connect to (with fingerprints)\n\n" + "connected: see connected peers to chat with\n\n" + "add;<ID>: connect with an available peer\n\n" + "<ID>;<message>: send a message to a connected peer\n";

    private final ChatClient chatClient;

    public SimpleTUI(ChatClient chatClient) {
        this.chatClient = chatClient;
        Thread tuiThread = new Thread(() -> {
            System.out.println(HELP);
            new BufferedReader(new InputStreamReader(System.in)).lines().forEach(line -> {
                try {
                    exec(line);
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            });
        });
        tuiThread.setName("TUI");
        tuiThread.setDaemon(true);
        tuiThread.start();
    }

    private void exec(String command) {
        String cmd = command.split(";")[0];
        switch (cmd) {
            case "available":
                for (IUser availableUser : this.chatClient.getNewUsers())
                    System.out.println(availableUser.toString());
                break;
            case "connected":
                for (IUser user : this.chatClient.getConnectedUsers())
                    System.out.println(user.toString());
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
    public void update(String message) {
        System.out.println(message);
    }
}
