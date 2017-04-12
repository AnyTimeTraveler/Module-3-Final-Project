package utwente.ns.chatlayer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;
import utwente.ns.tcp.RTP4Connection;
import utwente.ns.tcp.RTP4Layer;
import utwente.ns.tcp.RTP4Socket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by harindu on 4/12/17.
 */
@Log
public class ChatClient {
    
    public static final int MAX_AVAILABLE_PEER_COUNT = 10;
    public static final int BROADCAST_PORT = 1024;
    public static final int MESSAGE_PORT = 1025;
    
    private final RTP4Layer rtp4Layer;
    private final String name;
    private RTP4Socket socket;
    private String id;
    
    private KeyPair keyPair;
    
    private Map<String, PeerInfo> connectedPeers = new HashMap<>();
    private Map<String, PeerIdentity> availablePeers = new HashMap<>();
    
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
        // TODO
        return null;
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
    
    public void onPeerBroadcast(PeerIdentity identity, String fromAddress) {
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
    }
    
    public PeerIdentity[] getAvailablePeers() {
        Collection<PeerIdentity> peers = this.availablePeers.values();
        return peers.toArray(new PeerIdentity[peers.size()]);
    }
    
    public void run() {
        try {
            this.socket = this.rtp4Layer.open(BROADCAST_PORT);
        } catch (IOException e) {
            e.printStackTrace();
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

    public void sendChatMessage(ChatMessage message) {
        PeerInfo recipient = this.connectedPeers.get(message.getRecipientId());
        message.encryptContent(recipient.peerSharedKey);
        message.sign(this.keyPair.getPrivate());
        // RTP4Connection conn = this.rtp4Layer.connect(recipient.address, MESSAGE_PORT);
        // TODO: send message
    }
}
