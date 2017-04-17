package utwente.ns.applications;

import utwente.ns.NetworkStack;
import utwente.ns.chatlayer.ChatClient;
import utwente.ns.config.Config;
import utwente.ns.ui.UniversalComminucator;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by simon on 12.04.17.
 */
public class GUIChat implements IApplication {

    @Override
    public void start() {
        try {
            // Start the GUI
            JFrame frame = new JFrame("UniversalComminucator");
            UniversalComminucator universalComminucator = new UniversalComminucator();

            // Start the Network with default parameters
            NetworkStack networkStack = new NetworkStack();

            // Start the logic
            ChatClient chatClient = new ChatClient(getNameFromUser(), networkStack, universalComminucator, true);
            Thread chatThreat = new Thread(chatClient::run);
            chatThreat.setDaemon(true);
            chatThreat.setName("ChatThreat"); //Found the typo, but left it in. Have fun :)
            chatThreat.start();

            // Continue setup
            frame.setContentPane(universalComminucator.getMainPanel());
            universalComminucator.setup(chatClient);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();

            // Show it to the world!
            frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getNameFromUser() {
        return (String) JOptionPane.showInputDialog(null, "Please enter your name:", "Name", JOptionPane.QUESTION_MESSAGE, null, null, Config.getInstance().name);
    }
}
