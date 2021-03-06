package utwente.ns.filetransfer;

import utwente.ns.config.Config;
import utwente.ns.tcp.RTP4Connection;
import utwente.ns.tcp.RTP4Socket;
import utwente.ns.ui.UniversalCommunicator;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

/**
 * Created by simon on 17.04.17.
 */
public class FileTransferer {
    private static final int PORT = 21;
    private final UniversalCommunicator gui;
    private File file;
    private String address;

    /**
     * Send a file.
     *
     * @param gui     to track progress
     * @param file    to be successful
     * @param address to be successful to
     */
    public FileTransferer(UniversalCommunicator gui, File file, String address) {
        this.gui = gui;
        this.file = file;
        this.address = address;
        Thread transferThread = new Thread(this::sendTheFile);
        transferThread.setDaemon(true);
        transferThread.setName("FileTransfer");
        transferThread.start();
    }

    public FileTransferer(UniversalCommunicator gui) {
        this.gui = gui;
        Thread transferThread = new Thread(this::receiveTheFile);
        transferThread.setDaemon(true);
        transferThread.setName("FileTransfer");
        transferThread.start();
    }

    private void sendTheFile() {
        gui.addFileTransferLogMessage("Starting Transfer...");
        try {
            gui.addFileTransferLogMessage("Connecting...");
            RTP4Connection connection = gui.getNetworkStack().getRtp4Layer().connect(address, PORT);
            gui.addFileTransferLogMessage("Sending FileInfo...");
            connection.send(new FileInfoPacket(file.getName(), (int) file.length()).toBytes());
            FileInputStream fis = new FileInputStream(file);
            byte[] sendBuffer = new byte[Config.getInstance().filePartSize];
            for (int i = 0; i < file.length() / Config.getInstance().filePartSize + 1; i++) {
                gui.addFileTransferLogMessage("Sending Packet! (" + (i + 1) + "/" + (file.length() / Config.getInstance().filePartSize + 1) + ")");
                gui.setProgress(i, ((int) file.length()) / Config.getInstance().filePartSize + 1);
                int read = fis.read(sendBuffer);
                if (read < Config.getInstance().filePartSize) {
                    byte[] smallerSendBuffer = new byte[read];
                    System.arraycopy(sendBuffer, 0, smallerSendBuffer, 0, read);
                    try {
                        connection.send(smallerSendBuffer);
                    } catch (TimeoutException e) {
                        if (connection.remoteIsClosed()) {
                            gui.addFileTransferLogMessage("Error!");
                            return;
                        }
                        System.err.println("---> Retrying...");
                        e.printStackTrace();
                        // Retrying
                    }
                    connection.close();
                    gui.addFileTransferLogMessage("File successful!");
                    gui.setProgress(100, 100);
                    fis.close();
                } else {
                    try {
                        connection.send(sendBuffer);
                    } catch (TimeoutException e) {
                        if (connection.remoteIsClosed()) {
                            gui.addFileTransferLogMessage("Error!");
                            return;
                        }
                        System.err.println("---> Retrying...");
                        e.printStackTrace();
                        // Retrying
                    }
                }
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void receiveTheFile() {
        gui.addFileTransferLogMessage("Waiting for FileInfo...");
        try {
            RTP4Socket socket = gui.getNetworkStack().getRtp4Layer().open(PORT);
            RTP4Connection connection = socket.accept();
            byte[] fileInfoData = null;
            while (fileInfoData == null) {
                try {
                    fileInfoData = connection.receive();
                } catch (TimeoutException e) {
                    if (connection.remoteIsClosed()) {
                        gui.addFileTransferLogMessage("Error!");
                        return;
                    }
                    System.err.println("---> Retrying...");
                    e.printStackTrace();
                    // Retrying
                }
            }
            FileInfoPacket fileInfo = new FileInfoPacket(fileInfoData);
            gui.addFileTransferLogMessage("Received FileInfo!");
            JFileChooser fc = new JFileChooser();
            fc.setName(fileInfo.name);
            fc.showSaveDialog(gui.getMainPanel());
            File selectedFile = fc.getSelectedFile();
            FileOutputStream fos = new FileOutputStream(selectedFile);
            for (int i = 0; i < fileInfo.parts; i++) {
                byte[] part = new byte[0];
                try {
                    part = connection.receive();
                } catch (TimeoutException e) {
                    if (connection.remoteIsClosed()) {
                        gui.addFileTransferLogMessage("Error!");
                        return;
                    }
                    System.err.println("---> Retrying...");
                    e.printStackTrace();
                    // Retrying
                }
                if (part == null)
                    continue;
                fos.write(part);
                gui.addFileTransferLogMessage("Received Packet! (" + (i + 1) + "/" + fileInfo.parts + ")");
                gui.setProgress(i, fileInfo.parts);
            }
            gui.addFileTransferLogMessage("File received!");
            gui.setProgress(100, 100);
            connection.close();
            fos.flush();
            fos.close();
        } catch (IOException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private class FileInfoPacket {
        private final String name;
        private final int size;
        private final int parts;

        FileInfoPacket(String name, int size) {
            this.name = name;
            this.size = size;
            parts = size / Config.getInstance().filePartSize + 1;
        }

        private FileInfoPacket(byte[] bytes) {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            size = buf.getInt();
            byte[] nameData = new byte[bytes.length - 4];
            System.arraycopy(bytes, 4, nameData, 0, nameData.length);
            name = new String(nameData);
            parts = size / Config.getInstance().filePartSize + 1;
        }

        private byte[] toBytes() {
            byte[] data = name.getBytes();
            ByteBuffer buf = ByteBuffer.allocate(data.length + 4);
            buf.putInt(size);
            buf.put(data);
            return buf.array();
        }
    }
}
