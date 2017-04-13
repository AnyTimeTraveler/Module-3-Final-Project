package utwente.ns.applications;

import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.ip.HRP4Layer;
import utwente.ns.ip.HRP4Socket;
import utwente.ns.linklayer.SimulatedLinkLayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

/**
 * @author rhbvkleef
 *         Created on 4/12/17
 */
public class Chat implements IApplication {
    @Override
    public void start() {
        HRP4Layer layer;
        HRP4Socket socket;
        try {
            layer = new HRP4Layer(new SimulatedLinkLayer(2048));
            socket = layer.open((short) 25565);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        socket.addReceiveListener(packet -> System.out.println(new String(packet.getData())));

        new BufferedReader(new InputStreamReader(System.in)).lines().forEach(line -> {
            try {
                String destination = "192.168.w." + line.split(";")[0];
                String message = Config.getInstance().getMyAddress() + " --> " + destination +" : " + line.split(";")[1];
                InetAddress ip = InetAddress.getByName(destination);
                short port = 25565;
                socket.send(message.getBytes(), Util.addressToInt(ip), port);
            } catch (ArrayIndexOutOfBoundsException | IOException e) {
                e.printStackTrace();
            }
        });
    }
}
