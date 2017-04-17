package utwente.ns.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import utwente.ns.NetworkStack;
import utwente.ns.chatstructure.IChatController;
import utwente.ns.chatstructure.IConversation;
import utwente.ns.chatstructure.IUser;
import utwente.ns.chatstructure.IUserInterface;
import utwente.ns.config.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Created by simon on 12.04.17.
 */
public class UniversalComminucator implements IUserInterface {
    private JTabbedPane tabbedPane;
    @Getter
    private JPanel mainPanel;
    private JTextArea chatHistoryTextArea;
    private JList<String> contactList;
    private JTextField messageTextField;
    private JButton addContactButton;
    private JButton sendButton;
    private JList<String> settingsList;
    private JTextField settingsTextField;
    private JButton settingsSaveButton;
    private JList fileTransferUserList;
    private JButton fileTransferFileButton;
    private JButton fileTransferSendButton;
    private JProgressBar fileTransferProgressBar;
    private JButton createGroupButton;
    private JCheckBox settingsCheckBox;
    private JLabel settingsLabel;
    private JButton fileTransferReceiveButton;
    private JButton confirmIDButton;
    private JTextArea fileTransferLogTextArea;
    private JLabel restartNotice;
    private JPanel networkGraph;
    private HashMap<String, Field> settings;
    private IConversation selectedConversation;
    private IChatController chatClient;
    private NetworkStack networkStack;
    private boolean settingIsBoolean;

    @Override
    public void update(String message) {

        chatHistoryTextArea.setText("");
    }

    public void setup(IChatController chatClient, NetworkStack networkStack) {
        this.chatClient = chatClient;
        this.networkStack = networkStack;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // Chat
        contactList.setListData(new Vector<>(Arrays.stream(chatClient.getConnectedUsers())
                .map(user -> user.isConfirmed() ? "\\u2713" : "\\u2715" + user.getName())
                .collect(Collectors.toList())));


        messageTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    sendMessage();
                    messageTextField.setText("");
                } else
                    super.keyPressed(e);
            }
        });
        sendButton.addActionListener(e -> sendMessage());
        addContactButton.addActionListener(e -> {
            ConcurrentLinkedQueue<List<IUser>> selectionQueue = new ConcurrentLinkedQueue<>();
            new UserListDialogue(chatClient.getNewUsers(), true, selectionQueue);
            List<IUser> selection = selectionQueue.poll();
            if (!selection.isEmpty()) {
                selection.forEach(user -> chatClient.addPeerById(user.getUniqueID()));
            }
        });

        // File transfer
        fileTransferFileButton.addActionListener(e -> {

        });
        fileTransferSendButton.addActionListener(e -> {

        });
        fileTransferReceiveButton.addActionListener(e -> {

        });


        // Settings
        settings = new HashMap<>();
        restartNotice.setVisible(false);
        Field[] allFields = Config.class.getDeclaredFields();
        for (Field field : allFields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                settings.put(field.getName(), field);
            }
        }
        settingsList.setListData(new Vector<>(settings.keySet()));
        settingsList.addListSelectionListener(e -> {
            System.out.println("Selected!");
            System.out.print("Type: ");
            Field setting = settings.get(settingsList.getSelectedValue());
            System.out.println(setting.getType().toString().toLowerCase());
            switch (setting.getType().toString().toLowerCase()) {
                case "class java.lang.string":
                case "int":
                case "byte":
                    settingsLabel.setVisible(true);
                    settingsLabel.setText(setting.getName());
                    settingsTextField.setVisible(true);
                    try {
                        settingsTextField.setText(String.valueOf(setting.get(Config.getInstance())));
                    } catch (IllegalAccessException e1) {
                        // Tell user to PANIC!
                        e1.printStackTrace();
                    }
                    settingsCheckBox.setVisible(false);
                    settingIsBoolean = false;
                    break;
                case "boolean":
                    settingsLabel.setVisible(false);
                    settingsTextField.setVisible(false);
                    settingsCheckBox.setVisible(true);
                    settingsCheckBox.setText(setting.getName());
                    try {
                        settingsCheckBox.setSelected(setting.getBoolean(Config.getInstance()));
                    } catch (IllegalAccessException e1) {
                        // Tell user to PANIC!
                        e1.printStackTrace();
                    }
                    settingIsBoolean = true;
                    break;
                default:
                    System.err.print("Unknown Type: ");
                    System.err.println(setting.getType().toString().toLowerCase());
                    break;
            }
        });
        settingsSaveButton.addActionListener(e -> {
            try {
                Field setting = settings.get(settingsList.getSelectedValue());
                if (settingIsBoolean) {
                    if ((boolean) setting.get(Config.getInstance()) != settingsCheckBox.isSelected()) {
                        setting.setBoolean(Config.getInstance(), settingsCheckBox.isSelected());
                        restartNotice.setVisible(true);
                    }
                } else {
                    if (!String.valueOf(setting.get(Config.getInstance())).equals(settingsTextField.getText())) {
                        switch (setting.getType().toString().toLowerCase()) {
                            case "class java.lang.string":
                                setting.set(Config.getInstance(), settingsTextField.getText());
                                break;
                            case "int":
                                setting.setInt(Config.getInstance(), Integer.parseInt(settingsTextField.getText()));
                                break;
                            case "byte":
                                setting.setByte(Config.getInstance(), Byte.parseByte(settingsTextField.getText()));
                                break;
                            default:
                                System.err.println("Type not defined!");
                        }
                    }
                }
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (NumberFormatException e1) {
                JOptionPane.showMessageDialog(this.getMainPanel(), "Please enter something sensible!", "Incorrect Input!", JOptionPane.ERROR_MESSAGE);
            }
        });
        if (restartNotice.isVisible()) {
            // If settings were changed, save config.
            Config.getInstance().toFile();
        }
    }

    /**
     * Well, we will see about this function later...
     */
    private void sendMessage() {
        if (selectedConversation != null && !messageTextField.getText().trim().isEmpty()) {
            // TODO: Implement something for this
            // selectedConversation.sendMessage(new ChatMessage(chatClient.getId(),UUID.randomUUID().toString(),selectedConversation.getId(),null,ChatMessage.CONTENT_TYPE_TEXT,
            // messageTextField.getText()));
        }
    }

    private void createUIComponents() {
        networkGraph = new JPanel(new BorderLayout());
        NetworkGraph ng = new NetworkGraph(networkGraph);
        networkGraph.add(new JScrollPane(ng), BorderLayout.CENTER);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ng.updateNodes(networkStack.getHrp4Layer().getRouter().getRoutingEntries());
                ng.repaint();
            }
        }, Config.getInstance().baconInterval, Config.getInstance().baconInterval);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane = new JTabbedPane();
        mainPanel.add(tabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(600, 500), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Chat", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setEnabled(true);
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        contactList = new JList();
        contactList.setSelectionMode(2);
        scrollPane1.setViewportView(contactList);
        sendButton = new JButton();
        sendButton.setText("Send");
        panel2.add(sendButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createGroupButton = new JButton();
        createGroupButton.setText("CreateGroup");
        panel2.add(createGroupButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        confirmIDButton = new JButton();
        confirmIDButton.setText("Confirm ID");
        panel2.add(confirmIDButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addContactButton = new JButton();
        addContactButton.setText("AddContact");
        panel2.add(addContactButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.setEnabled(true);
        panel1.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(500, 54), null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(343, 17), null, 0, false));
        chatHistoryTextArea = new JTextArea();
        chatHistoryTextArea.setText("");
        scrollPane2.setViewportView(chatHistoryTextArea);
        messageTextField = new JTextField();
        messageTextField.setText("");
        panel3.add(messageTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(343, 25), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("File Transfer", panel4);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel5.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        fileTransferUserList = new JList();
        fileTransferUserList.setSelectionMode(0);
        scrollPane3.setViewportView(fileTransferUserList);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fileTransferFileButton = new JButton();
        fileTransferFileButton.setText("Select File");
        panel6.add(fileTransferFileButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileTransferSendButton = new JButton();
        fileTransferSendButton.setEnabled(false);
        fileTransferSendButton.setText("Send");
        panel6.add(fileTransferSendButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileTransferReceiveButton = new JButton();
        fileTransferReceiveButton.setText("Receive");
        panel6.add(fileTransferReceiveButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileTransferLogTextArea = new JTextArea();
        fileTransferLogTextArea.setText("");
        panel5.add(fileTransferLogTextArea, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Transfer Log");
        panel5.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileTransferProgressBar = new JProgressBar();
        panel4.add(fileTransferProgressBar, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Routing", panel7);
        panel7.add(networkGraph, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Settings", panel8);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        settingsTextField = new JTextField();
        settingsTextField.setText("");
        panel9.add(settingsTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel9.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        settingsSaveButton = new JButton();
        settingsSaveButton.setText("Save");
        panel10.add(settingsSaveButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel10.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        settingsCheckBox = new JCheckBox();
        settingsCheckBox.setText("");
        panel9.add(settingsCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        settingsLabel = new JLabel();
        settingsLabel.setText("Setting");
        panel9.add(settingsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        restartNotice = new JLabel();
        restartNotice.setText("Please restart application to apply settings!");
        panel9.add(restartNotice, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        scrollPane4.setEnabled(true);
        scrollPane4.setVerticalScrollBarPolicy(20);
        panel11.add(scrollPane4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        settingsList = new JList();
        settingsList.setSelectionMode(0);
        scrollPane4.setViewportView(settingsList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
