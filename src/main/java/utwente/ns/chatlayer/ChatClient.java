package utwente.ns.chatlayer;

import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.Util;
import utwente.ns.config.Config;
import utwente.ns.ip.HRP4Packet;
import utwente.ns.ip.HRP4Socket;
import utwente.ns.tcp.RTP4Layer;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Created by harindu on 4/12/17.
 */
@Log
public class ChatClient implements IReceiveListener {
    
    public static final int MAX_AVAILABLE_PEER_COUNT = 10;
    public static final short IDENTITY_PORT = 1024;
    public static final short MESSAGE_PORT = 1025;
    
    private final RTP4Layer rtp4Layer;
    private final String name;
    private HRP4Socket messageSocket;
    private HRP4Socket identitySocket;
    private String id;
    
    private KeyPair keyPair;
    
    private Map<String, PeerInfo> connectedPeers = new ConcurrentHashMap<>();
    private Map<String, PeerIdentity> availablePeers = new ConcurrentHashMap<>();
    
    public ChatClient(String name, RTP4Layer rtp4Layer) {
        
        this.name = name;
        this.rtp4Layer = rtp4Layer;
        
        // TODO: add ID generator
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(name.getBytes(StandardCharsets.UTF_8));
            this.id = Base64.getUrlEncoder().encodeToString(hash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.keyPair = CryptoUtil.generateKeyPair();
    }
    
    public String getOwnAddress() {
        return Config.getInstance().getMyAddress();
    }
    
    public PeerIdentity getIdentity() {
        return new PeerIdentity(id, name, this.getOwnAddress(), CryptoUtil.encodePublicKey(this.keyPair.getPublic()));
    }
    
    public void addPeer(PeerIdentity identity) throws InvalidKeySpecException, InvalidKeyException {
        
        // decode the public key of the peer
        PublicKey peerPublicKey = CryptoUtil.decodePublicKey(identity.publicKey);
        
        // generate shared key using own private key and peer public key
        Key peerKey = CryptoUtil.generateSharedKey(this.keyPair.getPrivate(), peerPublicKey);
        
        // add info to map
        this.connectedPeers.put(identity.id, new PeerInfo(identity.id, identity.name, identity.address, peerKey, peerPublicKey));
        
        // remove from available peers
        this.availablePeers.remove(identity.id);
    }
    
    public void onPeerIdentity(PeerIdentity identity, String fromAddress) {
        if (!identity.address.equals(fromAddress)) {
            log.log(Level.INFO, "Peer address mismatch; identity dropped");
            return;
        }
        if (this.connectedPeers.containsKey(identity.id))
            return;
        this.availablePeers.put(identity.id, identity);
    }

    
    private void dropOldestPeerIdentity() {
        PeerIdentity[] peers = getAvailablePeers();
        Arrays.sort(peers);
        if (peers.length > 1) {
            this.availablePeers.remove(peers[peers.length - 1].id);
        }
    }
    
    public PeerIdentity[] getAvailablePeers() {
        Collection<PeerIdentity> peers = this.availablePeers.values();
        return peers.toArray(new PeerIdentity[peers.size()]);
    }
    
    public void run() {
        try {
            this.messageSocket = this.rtp4Layer.getIpLayer().open(MESSAGE_PORT);
            this.identitySocket = this.rtp4Layer.getIpLayer().open(IDENTITY_PORT);
            Timer broadcastTimer = new Timer();
            broadcastTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendIdentityData();
                }
            }, (long) Config.getInstance().getBaconInterval() * 4, (long) Config.getInstance().getBaconInterval() * 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendIdentityData() {

        byte[] idData = Util.toJsonBytes(this.getIdentity());

        Set<Integer> peerAddrs = new HashSet<>();
        this.rtp4Layer.getIpLayer().getRouter().getRoutingEntries().forEach(entry -> {
            int[] addrs = entry.getBcn4Entry().getAddresses();
            peerAddrs.add(addrs[0]);
            peerAddrs.add(addrs[1]);
        });

        peerAddrs.forEach(addr -> {
            try {
                String addrStr = Util.intToAddressString(addr);
                if (addrStr.equals(this.getOwnAddress())) return;
                this.sendData(this.identitySocket, addrStr, IDENTITY_PORT, idData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void onIdentityData(String fromAddr, byte[] data) {
        PeerIdentity identity = Util.fromJsonBytes(data, PeerIdentity.class);
        if (identity == null) {
            log.log(Level.WARNING, "Peer identity JSON unmarshal was null");
            return;
        }
        this.onPeerIdentity(identity, fromAddr);
    }

    private void onMessageData(byte[] data) {
        ChatMessage message = Util.fromJsonBytes(data, ChatMessage.class);
        if (message == null) {
            log.log(Level.WARNING, "Message JSON unmarshal was null");
            return;
        }

        this.onMessage(message);
    }

    private void sendData(HRP4Socket sock, String toAddr, short toPort, byte[] data) throws IOException {
        if (sock == null)
            this.messageSocket.send(data, Util.addressStringToInt(toAddr), toPort);
        sock.send(data, Util.addressStringToInt(toAddr), toPort);
    }

    private void sendData(String toAddr, short toPort, byte[] data) throws IOException {
        this.sendData(null, toAddr, toPort, data);
    }

    public void sendChatMessage(ChatMessage message) throws IOException {
        PeerInfo recipient = this.connectedPeers.get(message.getRecipientId());

        if (recipient == null) {
            log.log(Level.WARNING, "Recipient not connected");
            return;
        }

        message.encryptContent(recipient.peerSharedKey);
        message.sign(this.keyPair.getPrivate());

        this.sendData(recipient.address, MESSAGE_PORT, Util.toJsonBytes(message));
    }

    public void onMessage(ChatMessage message) {
        PeerInfo sender = this.connectedPeers.get(message.getSenderId());

        if (sender == null) {
            log.log(Level.WARNING, "Message from unconnected sender");
            return;
        }

        if (!message.verify(sender.publicKey)) {
            log.log(Level.WARNING, "Message verification failed");
        }

        System.out.println(sender.name + ": " + (message.getContent() == null ? "NULL" : message.getContent().toString()));
    }

    @Override
    public void receive(IPacket packet) {
        if (!(packet instanceof HRP4Packet)) return;

        HRP4Packet hrp4Packet = (HRP4Packet) packet;

        switch (hrp4Packet.getDstPort()) {
            case IDENTITY_PORT:
                try {
                    this.onIdentityData(Util.intToAddressString(hrp4Packet.getSrcAddr()), hrp4Packet.getData());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            case MESSAGE_PORT:
                this.onMessageData(hrp4Packet.getData());
        }
    }

    @AllArgsConstructor
    private class PeerInfo {
        public final String id;
        public final String name;
        public final String address;
        public final Key peerSharedKey;
        public final PublicKey publicKey;
    }
}
