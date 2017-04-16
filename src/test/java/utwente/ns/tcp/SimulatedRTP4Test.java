package utwente.ns.tcp;

        import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import utwente.ns.ip.SimulatedHRP4Layer;

import java.io.IOException;

/**
 * Created by Niels Overkamp on 12-Apr-17.
 */
public class SimulatedRTP4Test {
    RTP4Layer rtp4LayerA;
    RTP4Layer rtp4LayerB;
    RTP4Connection connectionB;

    @Before
    public void setUp() throws Exception {
        SimulatedHRP4Layer hrp4Layer = new SimulatedHRP4Layer();
        new Thread(
                () -> {
                    try {
                        rtp4LayerA = new RTP4Layer(hrp4Layer);
                        RTP4Socket socketA = rtp4LayerA.open(5000);
                        RTP4Connection connection = socketA.accept();
                        System.out.println(Thread.currentThread().getName() + "> " + "Message : " + new String(connection.receive()));
                        connection.send("Hi Bae!".getBytes());
//                        while (!connectionB.isDone()) {
//                            Thread.sleep(1000);
//                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                "\tA")
                .start();
        Thread.currentThread().setName("B");
        rtp4LayerB = new RTP4Layer(hrp4Layer);
    }

    @Test
    public void test(){
        try {
            connectionB = rtp4LayerB.connect("",5000);
            connectionB.send("Hello Asshole".getBytes());
            System.out.println(Thread.currentThread().getName() + "> " + "Message : " +new String(connectionB.receive()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
//        while (!connectionB.isDone()) {
//            Thread.sleep(1000);
//        }
//        connectionB.close();
    }

}