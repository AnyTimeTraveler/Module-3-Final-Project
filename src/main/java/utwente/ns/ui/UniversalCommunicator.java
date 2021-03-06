package utwente.ns.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.vdurmont.emoji.EmojiParser;
import lombok.Getter;
import utwente.ns.NetworkStack;
import utwente.ns.chatstructure.IChatController;
import utwente.ns.chatstructure.IConversation;
import utwente.ns.chatstructure.IUser;
import utwente.ns.chatstructure.IUserInterface;
import utwente.ns.config.Config;
import utwente.ns.config.RequiresRestart;
import utwente.ns.filetransfer.FileTransferer;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by simon on 12.04.17.
 */
public class UniversalCommunicator implements IUserInterface {
    private JTabbedPane tabbedPane;
    @Getter
    private JPanel mainPanel;
    private JTextArea chatHistoryTextArea;
    private JList<String> conversationList;
    private JTextField messageTextField;
    private JButton addContactButton;
    private JButton sendButton;
    private JList<String> settingsList;
    private JTextField settingsTextField;
    private JButton settingsSaveButton;
    private JList<IUser> fileTransferUserList;
    private JButton fileTransferFileButton;
    private JButton fileTransferSendButton;
    private JProgressBar fileTransferProgressBar;
    private JCheckBox settingsCheckBox;
    private JLabel settingsLabel;
    private JButton fileTransferReceiveButton;
    private JTextArea fileTransferLogTextArea;
    private JLabel restartNotice;
    private JPanel networkGraph;
    private JLabel idField;
    private HashMap<String, Field> settings;
    private IConversation selectedConversation;
    private IChatController chatClient;
    @Getter
    private NetworkStack networkStack;
    private boolean settingIsBoolean;
    private File selectedFile;
    private NetworkGraph networkGrapher;
    private IConversation[] conversations;
    private IUser fileTransferSelectedUser;

    @Override
    public void update(String msg) {
        System.out.println(msg);
        if (selectedConversation != null)
            chatHistoryTextArea.setText(
                    Arrays.stream(selectedConversation.getChatHistory())
                            .map(message -> ((chatClient.getUserById(message.getSender())) != null ? chatClient.getUserById(message.getSender()).getName() : "ERROR") + " : " + message.getMessage())
                            .map(EmojiParser::parseToUnicode)
                            .collect(Collectors.joining("\n")));
        updateConversations(chatClient.getConversations());
        if (chatClient.getConnectedUsers().length > 0)
            fileTransferUserList.setListData(chatClient.getConnectedUsers());
    }

    public void setup(IChatController chatClient, NetworkStack networkStack) {
        this.chatClient = chatClient;
        this.networkStack = networkStack;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }
//        Font awesome = new JLabel().getFont();
//        try {
//            URL fp = getClass().getClassLoader().getResource("font-awesome.ttf");
//            File file = new File(fp.toURI());
//            awesome = Font.createFont(Font.TRUETYPE_FONT, file);
//            awesome = awesome.deriveFont(Font.PLAIN, 14);
//        } catch (FontFormatException | IOException | NullPointerException | URISyntaxException e) {
//            e.printStackTrace();
//        }

        // Chat
//        chatHistoryTextArea.setFont(awesome);
        updateConversations(chatClient.getConversations());
        conversationList.addListSelectionListener(e -> {
            for (IConversation con : chatClient.getConversations()) {
                if (con.getName().equals(conversationList.getSelectedValue())) {
                    selectedConversation = con;
                    break;
                }
            }
            if (selectedConversation != null)
                chatHistoryTextArea.setText(Arrays.stream(selectedConversation.getChatHistory()).map(message -> ((chatClient.getUserById(message.getSender())) != null ? chatClient.getUserById(message.getSender()).getName() : "ERROR") + " : " + message.getMessage()).collect(Collectors.joining("\n")));
        });
//        createGroupButton.addActionListener(e -> {
//            Thread t = new Thread(() -> {
//                LinkedBlockingQueue<List<IUser>> selectionQueue = new LinkedBlockingQueue<>();
//                new Thread(() -> new UserListDialogue(chatClient.getConnectedUsers(), true, selectionQueue)).start();
//                List<IUser> selection = null;
//                try {
//                    selection = selectionQueue.poll(1, TimeUnit.MINUTES);
//                } catch (InterruptedException e1) {
//                    e1.printStackTrace();
//                }
//                if (selection != null && !selection.isEmpty()) {
//                    String name = (String) JOptionPane.showInputDialog(null, "Please enter a name for the group: ", "Group Name", JOptionPane.QUESTION_MESSAGE, null, null, "Unnamed Group");
//                    if (name != null)
//                        chatClient.addConversation(name, selection.toArray(new IUser[selection.size()]));
//                }
//            });
//            t.setName("Dialogue");
//            t.start();
//        });
        messageTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '\n' && selectedConversation != null && !messageTextField.getText().trim().isEmpty()) {
                    sendMessage();
                } else
                    super.keyPressed(e);
            }
        });
//        messageTextField.setFont(awesome);
        sendButton.addActionListener(e -> {
            if (selectedConversation != null && !messageTextField.getText().trim().isEmpty()) {
                sendMessage();
            }
        });
        addContactButton.addActionListener(e -> {
            Thread t = new Thread(() -> {
                LinkedBlockingQueue<List<IUser>> selectionQueue = new LinkedBlockingQueue<>();
                new UserListDialogue(chatClient.getNewUsers(), true, selectionQueue);
                List<IUser> selection = null;
                try {
                    selection = selectionQueue.poll(1, TimeUnit.MINUTES);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                if (selection != null && !selection.isEmpty()) {
                    selection.forEach(user -> {
                        chatClient.addPeerById(user.getUniqueID());
                        chatClient.addConversation(user.getName(), user);
                    });
                }
            });
            t.setName("Dialogue");
            t.start();
        });

        DefaultCaret chatCaret = (DefaultCaret) chatHistoryTextArea.getCaret();
        chatCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        // File transfer
        fileTransferFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.showOpenDialog(this.getMainPanel());
            selectedFile = fc.getSelectedFile();
            if (selectedFile != null && fileTransferSelectedUser != null) {
                fileTransferSendButton.setEnabled(true);
            }
        });
        fileTransferSendButton.addActionListener(e -> new FileTransferer(this, selectedFile, fileTransferSelectedUser.getAddress()));
        fileTransferReceiveButton.addActionListener(e -> new FileTransferer(this));
        fileTransferUserList.addListSelectionListener(e -> {
            fileTransferSelectedUser = fileTransferUserList.getSelectedValue();
            if (selectedFile != null && fileTransferSelectedUser != null) {
                fileTransferSendButton.setEnabled(true);
            }
        });

        DefaultCaret logCaret = (DefaultCaret) fileTransferLogTextArea.getCaret();
        logCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

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
            Field setting = settings.get(settingsList.getSelectedValue());
            switch (setting.getType().toString().toLowerCase()) {
                case "class java.lang.string":
                case "int":
                case "byte":
                case "long":
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
                    settingsLabel.setVisible(false);
                    settingsTextField.setVisible(false);
                    settingsCheckBox.setVisible(false);
                    System.err.print("Unknown Type: ");
                    System.err.println(setting.getType().toString().toLowerCase());
                    break;
            }
            saveConfigIfNeeded();
        });
        tabbedPane.addChangeListener(e -> saveConfigIfNeeded());
        settingsCheckBox.addChangeListener(e -> saveConfigIfNeeded());
        settingsTextField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                saveConfigIfNeeded();
            }
        });
        // Routing
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                networkGrapher.updateNodes(networkStack.getHrp4Layer().getRouter().getRoutingEntries());
                networkGrapher.repaint();
            }
        }, Config.getInstance().baconInterval, Config.getInstance().baconInterval);
    }

    private void saveConfigIfNeeded() {
        try {
            Field setting = settings.get(settingsList.getSelectedValue());
            if (setting == null) {

                return;
            }
            boolean isRestartRequired = setting.getAnnotationsByType(RequiresRestart.class).length != 0;
            if (settingIsBoolean) {
                if ((boolean) setting.get(Config.getInstance()) != settingsCheckBox.isSelected()) {
                    setting.setBoolean(Config.getInstance(), settingsCheckBox.isSelected());
                    if (isRestartRequired) {
                        restartNotice.setVisible(true);
                    }
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
						case "long":
							setting.setLong(Config.getInstance(), Long.parseLong(settingsTextField.getText()));
							break;
                        default:
                            System.err.println("Type not defined!");
                    }
					if (isRestartRequired) {
						restartNotice.setVisible(true);
					}
                }
            }
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (NumberFormatException e1) {
            JOptionPane.showMessageDialog(this.getMainPanel(), "Please enter something sensible!", "Incorrect Input!", JOptionPane.ERROR_MESSAGE);
        }
        Config.getInstance().toFile();
    }

    private void updateConversations(IConversation[] conversations) {
        if (this.conversations == null || this.conversations.length != conversations.length) {
            int index = conversationList.getSelectedIndex();
            conversationList.setListData(new Vector<>(Arrays.stream(conversations)
                    .map(IConversation::getName).collect(Collectors.toSet())));
            if (index < conversations.length - 1)
                conversationList.setSelectedIndex(index);
            this.conversations = conversations;
        }
    }

    public void setProgress(int current, int max) {
        fileTransferProgressBar.setMinimum(0);
        fileTransferProgressBar.setMaximum(max);
        fileTransferProgressBar.setValue(current);
    }

    public void addFileTransferLogMessage(String message) {
        fileTransferLogTextArea.setText(fileTransferLogTextArea.getText() + '\n' + message);
    }

    /**
     * Well, we will see about this function later...
     */
    private void sendMessage() {
        Thread sendMessageThread = new Thread(() -> {
            selectedConversation.sendMessage(messageTextField.getText());
            messageTextField.setText("");
            update("Sent message!");
        });
        sendMessageThread.setDaemon(true);
        sendMessageThread.setName("MessageSinding Thread");
        sendMessageThread.start();
    }

    private void createUIComponents() {
        networkGraph = new JPanel(new BorderLayout());
        networkGrapher = new NetworkGraph(networkGraph);
        networkGraph.add(new JScrollPane(networkGrapher), BorderLayout.CENTER);
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
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Routing", panel1);
        panel1.add(networkGraph, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Chat", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.setEnabled(true);
        panel2.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        conversationList = new JList();
        conversationList.setSelectionMode(0);
        scrollPane1.setViewportView(conversationList);
        sendButton = new JButton();
        sendButton.setText("Send");
        panel3.add(sendButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addContactButton = new JButton();
        addContactButton.setText("AddContact");
        panel3.add(addContactButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.setEnabled(true);
        panel2.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(500, 54), null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        scrollPane2.setVerticalScrollBarPolicy(22);
        panel4.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(343, 17), null, 0, false));
        chatHistoryTextArea = new JTextArea();
        chatHistoryTextArea.setEditable(false);
        chatHistoryTextArea.setLineWrap(true);
        chatHistoryTextArea.setText("");
        chatHistoryTextArea.setWrapStyleWord(true);
        scrollPane2.setViewportView(chatHistoryTextArea);
        messageTextField = new JTextField();
        messageTextField.setText("");
        panel4.add(messageTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(343, 25), null, 0, false));
        idField = new JLabel();
        idField.setText("");
        panel2.add(idField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("File Transfer", panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel6.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        fileTransferUserList = new JList();
        fileTransferUserList.setSelectionMode(0);
        scrollPane3.setViewportView(fileTransferUserList);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fileTransferFileButton = new JButton();
        fileTransferFileButton.setText("Select File");
        panel7.add(fileTransferFileButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileTransferSendButton = new JButton();
        fileTransferSendButton.setEnabled(false);
        fileTransferSendButton.setText("Send");
        panel7.add(fileTransferSendButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileTransferReceiveButton = new JButton();
        fileTransferReceiveButton.setText("Receive");
        panel7.add(fileTransferReceiveButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Transfer Log");
        panel6.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        scrollPane4.setHorizontalScrollBarPolicy(31);
        scrollPane4.setVerticalScrollBarPolicy(22);
        panel6.add(scrollPane4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        fileTransferLogTextArea = new JTextArea();
        fileTransferLogTextArea.setEditable(false);
        fileTransferLogTextArea.setText("");
        scrollPane4.setViewportView(fileTransferLogTextArea);
        fileTransferProgressBar = new JProgressBar();
        panel5.add(fileTransferProgressBar, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Settings", panel8);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        settingsTextField = new JTextField();
        settingsTextField.setText("");
        panel9.add(settingsTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel9.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        settingsCheckBox = new JCheckBox();
        settingsCheckBox.setText("");
        panel9.add(settingsCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        settingsLabel = new JLabel();
        settingsLabel.setText("Setting");
        panel9.add(settingsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        restartNotice = new JLabel();
        restartNotice.setText("Please restart application to apply settings!");
        panel9.add(restartNotice, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        scrollPane5.setEnabled(true);
        scrollPane5.setVerticalScrollBarPolicy(20);
        panel10.add(scrollPane5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        settingsList = new JList();
        settingsList.setSelectionMode(0);
        scrollPane5.setViewportView(settingsList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
