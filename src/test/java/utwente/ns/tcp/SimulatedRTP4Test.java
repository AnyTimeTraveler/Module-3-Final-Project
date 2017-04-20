package utwente.ns.tcp;

        import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import utwente.ns.ip.SimulatedHRP4Layer;

import java.io.IOException;
        import java.util.concurrent.TimeoutException;

        import static org.junit.Assert.assertEquals;
        import static org.junit.Assert.assertTrue;

/**
 * Created by Niels Overkamp on 12-Apr-17.
 */
public class SimulatedRTP4Test {
    RTP4Layer rtp4LayerA;
    RTP4Layer rtp4LayerB;
    RTP4Connection connectionB;
    SimulatedHRP4Layer hrp4Layer;

    @Before
    public void setUp() throws Exception {
        hrp4Layer = new SimulatedHRP4Layer();
    }

    @Test
    public void test50Packets() throws IOException, TimeoutException, InterruptedException {
        new Thread(
                () -> {
                    try {
                        rtp4LayerA = new RTP4Layer(hrp4Layer);
                        RTP4Socket socketA = rtp4LayerA.open(5000);
                        RTP4Connection connection = socketA.accept();
                        byte count = 0;
                        while (!connection.remoteIsClosed()) {
                            byte[] data = connection.receive();
                            if (data == null) {
                                break;
                            }
                            System.out.println(Thread.currentThread().getName() + "> " + "Message : " + new String(data));
                            assertEquals(new String(data), String.valueOf(count));
                            count++;
                            count %= 128;
                        }
                        assertEquals(count, 50);
                        connection.close();
                        assertTrue(connection.localIsClosed());
                        while (connection.getState() != RTP4Layer.ConnectionState.CLOSED) {
                            Thread.sleep(1000);
                        }
                    } catch (IOException | InterruptedException | TimeoutException e) {
                        e.printStackTrace();
                    }
                },
                "\tA")
                .start();
        Thread.currentThread().setName("B");
        rtp4LayerB = new RTP4Layer(hrp4Layer);
        connectionB = rtp4LayerB.connect("",5000);
        byte count = 0;
        for (int i = 0; i < 50; i++) {
            connectionB.send(String.valueOf(count).getBytes());
            count++;
            count %= 128;
        }
    }

    @Test
//    public void testBigPackets() throws Exception {
//        new Thread(
//                () -> {
//                    try {
//                        rtp4LayerA = new RTP4Layer(hrp4Layer);
//                        RTP4Socket socketA = rtp4LayerA.open(5000);
//                        RTP4Connection connection = socketA.accept();
//                        byte count = 0;
//                        while (!connection.remoteIsClosed()) {
//                            byte[] data = connection.receive();
//                            if (data == null) {
//                                break;
//                            }
//                            System.out.println(Thread.currentThread().getName() + "> " + "Message : " + new String(data));
//                            assertEquals(data[0], count);
//                            count++;
//                            count %= 128;
//                        }
//                        assertEquals(count, 50);
//                        connection.close();
//                        assertTrue(connection.localIsClosed());
//                    } catch (IOException | InterruptedException | TimeoutException e) {
//                        e.printStackTrace();
//                    }
//                },
//                "\tA")
//                .start();
//        Thread.currentThread().setName("B");
//        rtp4LayerB = new RTP4Layer(hrp4Layer);
//        connectionB = rtp4LayerB.connect("",5000);
//        byte count = 0;
//        byte[] data;
//        for (int i = 0; i < 10; i++) {
//            data = new byte[1000];
//            data[0] = count;
//            connectionB.send(data);
//            count++;
//            count %= 128;
//        }
//    }

    @After
    public void tearDown() throws Exception {
        connectionB.close();
        while (connectionB.getState() != RTP4Layer.ConnectionState.CLOSED) {
            Thread.sleep(1000);
        }
    }

}