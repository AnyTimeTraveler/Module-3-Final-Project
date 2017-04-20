package utwente.ns.linklayer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by simon on 07.04.17.
 */
public class VirtualLinkLayerTest {
    private VirtualLinkLayer linkLayer;
    private boolean sent;

    @Before
    public void setUp() throws Exception {
        linkLayer = new VirtualLinkLayer(2048);
        sent = false;
    }

    @Test
    public void connectionTest() throws IOException, InterruptedException {
        linkLayer.addReceiveListener(packet -> {
            String x = new String(packet.getData());
            System.out.println(x);
            sent = true;
        });
        linkLayer.send("Correct!!!".getBytes());
        Thread.sleep(200);
        while (!sent) {
            System.out.println("Waiting...");
            Thread.sleep(1000);
        }
    }

    @After
    public void tearDown() throws Exception {
        linkLayer.close();
    }

}
