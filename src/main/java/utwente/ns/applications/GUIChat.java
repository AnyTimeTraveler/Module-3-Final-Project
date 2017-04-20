package utwente.ns.applications;

import utwente.ns.NetworkStack;
import utwente.ns.chatlayer.ChatClient;
import utwente.ns.config.Config;
import utwente.ns.ui.UniversalCommunicator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Created by simon on 12.04.17.
 */
public class GUIChat implements IApplication {

    @Override
    public void start() {
//        try {
//            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//            ge.registerFont();
//        } catch (IOException | FontFormatException e) {
//            //Handle exception
//        }

        if (Config.getInstance().myAddress.equals("UNCONFIGURED")) {
            Config.getInstance().myAddress = JOptionPane.showInputDialog("Enter your IP Address:");
            Config.getInstance().toFile();
        }

        try {
            // Start the GUI
            UniversalCommunicator universalComminucator = new UniversalCommunicator();

            // Start the Network with default parameters
            NetworkStack networkStack = new NetworkStack();

            // Start the logic
            ChatClient chatClient = new ChatClient(getNameFromUser(), networkStack, universalComminucator, true);
            Thread chatThreat = new Thread(chatClient::run);
            chatThreat.setDaemon(true);
            chatThreat.setName("ChatThreat"); //Found the typo, but left it in. Have fun :)
            chatThreat.start();
            EventQueue.invokeLater(() -> {
                JFrame frame = new JFrame("UniversalCommunicator");

                // Continue setup
                frame.setContentPane(universalComminucator.getMainPanel());
                universalComminucator.setup(chatClient, networkStack);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationByPlatform(true);

                // Show it to the world!
                frame.setVisible(true);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getNameFromUser() {
        return (String) JOptionPane.showInputDialog(null, "Please enter your name:", "Name", JOptionPane.QUESTION_MESSAGE, null, null, Config.getInstance().name);
    }
}
