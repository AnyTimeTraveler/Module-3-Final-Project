package utwente.ns.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class Config {

    // Configfile name
    private static final String CONFIGFILE = "config.json";
    private static Config instance;

    public String multicastAddress;
    public int multicastPort;
    public int baconInterval;
    public byte baconPacketTTL;
    public String myAddress;
    public byte defaultHRP4TTL;
    public int segmentBufferSize;
    public String name;
    public int maxSegmentLife;
    public int tcpPacketTimeout;
    public int tcpListenTimeout;
    public int tcpPacketInterval;

    private Config() {
        multicastAddress = "228.0.0.1";
        multicastPort = 1337;
        baconInterval = 1000;
        baconPacketTTL = 4;
        myAddress = "CHANGE ME, I'M DEFINITELY NOT CONFIGURED YET!";
        defaultHRP4TTL = 6;
        segmentBufferSize = 2048;
        name = "UNSET";
        maxSegmentLife = 5;
        maxSegmentLife = 2*60000;
        tcpPacketTimeout = 5;
        tcpListenTimeout = 60000;
        tcpPacketInterval = 10;
    }

    public static Config getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    private static void load(File file) {
        instance = fromFile(file);
        // no config file found
        if (instance == null) {
            instance = fromDefaults();
            instance.toFile(CONFIGFILE);
            throw new RuntimeException("Set values in config file according to your settings!");
        }
    }

    private static void load() {
        load(new File(CONFIGFILE));
    }

    private static Config fromDefaults() {
        return new Config();
    }

    private static Config fromFile(File configFile) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
            return gson.fromJson(reader, Config.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void toFile() {
        toFile(new File(CONFIGFILE));
    }

    public void toFile(String file) {
        toFile(new File(file));
    }

    public void toFile(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonConfig = gson.toJson(this);
        FileWriter writer;
        try {
            writer = new FileWriter(file);
            writer.write(jsonConfig);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}