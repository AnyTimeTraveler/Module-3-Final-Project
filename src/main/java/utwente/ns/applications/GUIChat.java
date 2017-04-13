package utwente.ns.applications;

import utwente.ns.gui.UniversalComminucator;

import javax.swing.*;

/**
 * Created by simon on 12.04.17.
 */
public class GUIChat implements IApplication {

    @Override
    public void start() {
        JFrame frame = new JFrame("UniversalComminucator");
        UniversalComminucator universalComminucator = new UniversalComminucator();
        frame.setContentPane(universalComminucator.getMainPanel());
        universalComminucator.setup();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
