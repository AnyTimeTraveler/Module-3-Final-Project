package utwente.ns.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import utwente.ns.chatstructure.IGui;
import utwente.ns.config.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by simon on 12.04.17.
 */
public class UniversalComminucator implements IGui {
    private JTabbedPane tabbedPane;
    @Getter
    private JPanel mainPanel;
    private JTextArea chatHistoryTextArea;
    private JList<String> clientList;
    private JTextField messageTextField;
    private JButton sendPrivateButton;
    private JButton sendButton;
    private JList<String> settingsList;
    private JTextField settingsTextField;
    private JButton saveButton;
    private HashMap<String, Field> settings;

    @Override
    public void update() {

    }

    public void setup() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }
        settings = new HashMap<>();
        Field[] allFields = Config.class.getDeclaredFields();
        for (Field field : allFields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                settings.put(field.getName(), field);
            }
        }
        settingsList.setListData(new Vector<>(settings.keySet()));

        messageTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        sendPrivateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane = new JTabbedPane();
        mainPanel.add(tabbedPane,
                      new GridConstraints(0,
                                          0,
                                          1,
                                          1,
                                          GridConstraints.ANCHOR_CENTER,
                                          GridConstraints.FILL_BOTH,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                          null,
                                          new Dimension(200, 200),
                                          null,
                                          0,
                                          false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Chat", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2,
                   new GridConstraints(0,
                                       1,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_WEST,
                                       GridConstraints.FILL_VERTICAL,
                                       1,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       new Dimension(100, -1),
                                       new Dimension(100, -1),
                                       new Dimension(100, -1),
                                       0,
                                       false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1,
                   new GridConstraints(0,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_BOTH,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        clientList = new JList();
        clientList.setSelectionMode(0);
        scrollPane1.setViewportView(clientList);
        sendPrivateButton = new JButton();
        sendPrivateButton.setText("Wisper");
        panel2.add(sendPrivateButton,
                   new GridConstraints(1,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_HORIZONTAL,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       GridConstraints.SIZEPOLICY_FIXED,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        sendButton = new JButton();
        sendButton.setText("Send");
        panel2.add(sendButton,
                   new GridConstraints(2,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_HORIZONTAL,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       GridConstraints.SIZEPOLICY_FIXED,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3,
                   new GridConstraints(0,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_BOTH,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2,
                   new GridConstraints(0,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_BOTH,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        chatHistoryTextArea = new JTextArea();
        chatHistoryTextArea.setText("ChatHistory");
        scrollPane2.setViewportView(chatHistoryTextArea);
        messageTextField = new JTextField();
        messageTextField.setText("Message");
        panel3.add(messageTextField,
                   new GridConstraints(1,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_WEST,
                                       GridConstraints.FILL_HORIZONTAL,
                                       GridConstraints.SIZEPOLICY_WANT_GROW,
                                       GridConstraints.SIZEPOLICY_FIXED,
                                       null,
                                       new Dimension(150, -1),
                                       null,
                                       0,
                                       false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Routing", panel4);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Settings", panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6,
                   new GridConstraints(0,
                                       1,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_BOTH,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        settingsTextField = new JTextField();
        settingsTextField.setText("Settings");
        panel6.add(settingsTextField,
                   new GridConstraints(0,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_WEST,
                                       GridConstraints.FILL_HORIZONTAL,
                                       GridConstraints.SIZEPOLICY_WANT_GROW,
                                       GridConstraints.SIZEPOLICY_FIXED,
                                       null,
                                       new Dimension(150, -1),
                                       null,
                                       0,
                                       false));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1,
                   new GridConstraints(1,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_VERTICAL,
                                       1,
                                       GridConstraints.SIZEPOLICY_WANT_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7,
                   new GridConstraints(2,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_BOTH,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        saveButton = new JButton();
        saveButton.setText("Save");
        panel7.add(saveButton,
                   new GridConstraints(0,
                                       1,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_HORIZONTAL,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       GridConstraints.SIZEPOLICY_FIXED,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        final Spacer spacer2 = new Spacer();
        panel7.add(spacer2,
                   new GridConstraints(0,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_HORIZONTAL,
                                       GridConstraints.SIZEPOLICY_WANT_GROW,
                                       1,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel8,
                   new GridConstraints(0,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_WEST,
                                       GridConstraints.FILL_VERTICAL,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        final JScrollPane scrollPane3 = new JScrollPane();
        scrollPane3.setEnabled(true);
        scrollPane3.setVerticalScrollBarPolicy(20);
        panel8.add(scrollPane3,
                   new GridConstraints(0,
                                       0,
                                       1,
                                       1,
                                       GridConstraints.ANCHOR_CENTER,
                                       GridConstraints.FILL_BOTH,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                       null,
                                       null,
                                       null,
                                       0,
                                       false));
        settingsList = new JList();
        settingsList.setSelectionMode(0);
        scrollPane3.setViewportView(settingsList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
