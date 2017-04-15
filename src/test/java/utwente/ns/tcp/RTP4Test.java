package utwente.ns.tcp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import utwente.ns.NetworkStack;

import java.io.IOException;

/**
 * Created by simon on 13.04.17.
 */
public class RTP4Test {

    private NetworkStack stack;

    private String partner = "192.168.5.1";

    @Before
    public void setUp() throws Exception {
        stack = new NetworkStack();
    }

    @Test
    public void client() throws IOException, InterruptedException {
        RTP4Connection connection = stack.getRtp4Layer().connect(partner, 10);

        connection.send("Server: Hi!".getBytes());
        Assert.assertEquals("First message from client", "Client: Hello", new String(connection.receive()));
    }

    @Test
    public void server() throws IOException, InterruptedException {
        RTP4Connection connection = stack.getRtp4Layer().open(10).accept();

        Assert.assertEquals("First message from server", "Server: Hi!", new String(connection.receive()));
        connection.send("Client: Hello".getBytes());
    }

    @After
    public void tearDown() throws Exception {
    }

}