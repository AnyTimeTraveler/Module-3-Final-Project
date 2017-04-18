package utwente.ns.chatlayer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;
import utwente.ns.IPacket;
import utwente.ns.IReceiveListener;
import utwente.ns.NetworkStack;
import utwente.ns.Util;
import utwente.ns.chatstructure.IChatController;
import utwente.ns.chatstructure.IConversation;
import utwente.ns.chatstructure.IUser;
import utwente.ns.chatstructure.IUserInterface;
import utwente.ns.config.Config;
import utwente.ns.ip.HRP4Packet;
import utwente.ns.ip.IHRP4Socket;
import utwente.ns.ui.SimpleTUI;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Created by Harindu Perera on 4/12/17.
 */
@Log
public class ChatClient implements IReceiveListener, IChatController {

    public static final int MAX_AVAILABLE_PEER_COUNT = 10;
    public static final short IDENTITY_PORT = 1024;
    public static final short MESSAGE_PORT = 1025;
    public static final long AVAILABLE_PEER_EXPIRY_TIME_MS = 1000 * 6; // 6 seconds
    public static final long CONNECTED_PEER_EXPIR_TIME_MS = 1000 * 30; // 30 seconds

    private final NetworkStack networkStack;
    private final String name;
    private final boolean isGUI;
    private final IUserInterface ui;
    private IHRP4Socket messageSocket;
    private IHRP4Socket identitySocket;
    private String id;
    private List<ChatConversation> conversations = new LinkedList<>();
    private Map<String, ChatConversation> conversationMap = new HashMap<>();

    private KeyPair keyPair;

    private Map<String, PeerInfo> connectedPeers = new ConcurrentHashMap<>();
    private Map<String, PeerIdentity> availablePeers = new ConcurrentHashMap<>();

    public ChatClient(String name, NetworkStack networkStack) {
        this.name = name;
        this.networkStack = networkStack;
        this.id = UUID.randomUUID().toString();
        this.keyPair = CryptoUtil.generateKeyPair();
        this.ui = new SimpleTUI(this);
        isGUI = false;
    }

    public ChatClient(String name, NetworkStack networkStack, IUserInterface gui, boolean isGUI) {
        this.name = name;
        this.networkStack = networkStack;
        this.id = UUID.randomUUID().toString();
        this.keyPair = CryptoUtil.generateKeyPair();
        this.ui = gui;
        this.isGUI = isGUI;
    }

    public String getOwnAddress() {
        return Config.getInstance().myAddress;
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
        this.connectedPeers.put(identity.id, new PeerInfo(identity.id, identity.name, identity.address, peerKey, peerPublicKey, identity.getFingerprint(), new Date()));

        // remove from available peers
        this.availablePeers.remove(identity.id);
    }

    @Override
    public boolean addPeerById(String id) {
        PeerIdentity peerIdentity = this.availablePeers.get(id);
        if (peerIdentity == null)
            return false;
        try {
            this.addPeer(peerIdentity);
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            return false;
        }
        return true;
    }

    public void onPeerIdentity(PeerIdentity identity, String fromAddress) {
        if (!identity.address.equals(fromAddress)) {
            log.log(Level.INFO, "Peer address mismatch; identity dropped");
            return;
        }
        if (this.connectedPeers.containsKey(identity.id)) {
            if (this.connectedPeers.get(identity.id).getFingerprint().equals(identity.getFingerprint())) {
                this.connectedPeers.get(identity.id).onUpdate();
            }
            return;
        }
        this.availablePeers.put(identity.id, identity);
        this.dropOldestPeer();
    }


    private void dropOldestPeer() {
        if (this.availablePeers.size() < MAX_AVAILABLE_PEER_COUNT)
            return;
        PeerIdentity[] peers = getAvailablePeers();
        Arrays.sort(peers);
        this.availablePeers.remove(peers[peers.length - 1].id);
    }

    private void dropExpiredPeers() {
        List<String> expiredAvailableIDs = new LinkedList<>();
        final Date availableExpiryTime = new Date(System.currentTimeMillis() - AVAILABLE_PEER_EXPIRY_TIME_MS);
        this.availablePeers.forEach((id, peer) -> {
            if (peer.updateTime.before(availableExpiryTime))
                expiredAvailableIDs.add(id);
        });
        expiredAvailableIDs.forEach(id -> {
            this.availablePeers.remove(id);
        });

        List<String> expiredConnectedIDs = new LinkedList<>();
        Date connectionExpiryTime = new Date(System.currentTimeMillis() - CONNECTED_PEER_EXPIR_TIME_MS);
        this.connectedPeers.forEach((id, peer) -> {
            if (peer.lastUpdateTime.before(connectionExpiryTime))
                expiredConnectedIDs.add(id);
        });
        expiredConnectedIDs.forEach(id -> {
            this.connectedPeers.remove(id);
        });
    }

    public PeerIdentity[] getAvailablePeers() {
        Collection<PeerIdentity> peers = this.availablePeers.values();
        return peers.toArray(new PeerIdentity[peers.size()]);
    }

    public void run() {
        try {
            this.messageSocket = this.networkStack.getRtp4Layer().getIpLayer().open(MESSAGE_PORT);
            this.identitySocket = this.networkStack.getRtp4Layer().getIpLayer().open(IDENTITY_PORT);
            this.messageSocket.addReceiveListener(this);
            this.identitySocket.addReceiveListener(this);

            Timer broadcastTimer = new Timer();
            broadcastTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendIdentityData();
                    dropExpiredPeers();
                }
            }, Config.getInstance().baconInterval * 4, Config.getInstance().baconInterval * 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendIdentityData() {
        byte[] idData = Util.toJsonBytes(this.getIdentity());

        Set<Integer> peerAddrs = new HashSet<>();
        this.networkStack.getRtp4Layer().getIpLayer().getRouter().getRoutingEntries().forEach(entry -> {
            int[] addrs = entry.getBcn4Entry().getAddresses();
            peerAddrs.add(addrs[0]);
            peerAddrs.add(addrs[1]);
        });

        peerAddrs.forEach(addr -> {
            try {
                String addrStr = Util.intToAddressString(addr);
                if (addrStr.equals(this.getOwnAddress()))
                    return;
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

    private void sendData(IHRP4Socket sock, String toAddr, short toPort, byte[] data) throws IOException {
        if (sock == null)
            this.messageSocket.send(data, Util.addressStringToInt(toAddr), toPort);
        else
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

        this.sendData(recipient.address, MESSAGE_PORT, Util.toJsonBytes(message));
    }

    private void onMessage(ChatMessage message) {
        ChatConversation conversation = message.getGroupId() == null ? this.getDirectConversation(message.getSenderId()) : this.getGroupConversation(message.getGroupId());

        if (conversation == null) {
            log.log(Level.WARNING, "Message from unknown group or unconnected sender; dropped");
            return;
        }

        conversation.onNewMessage(message);
    }

    @Override
    public void receive(IPacket packet) {
        if (!(packet instanceof HRP4Packet))
            return;

        HRP4Packet hrp4Packet = (HRP4Packet) packet;

        switch (hrp4Packet.getDstPort()) {
            case IDENTITY_PORT:
                try {
                    this.onIdentityData(Util.intToAddressString(hrp4Packet.getSrcAddr()), hrp4Packet.getData());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case MESSAGE_PORT:
                this.onMessageData(hrp4Packet.getData());
                break;
        }
    }

    private void addConversation(String key, ChatConversation conversation) {
        synchronized (conversationMap) {
            this.conversationMap.put(key, conversation);
            this.conversations.add(conversation);
        }
    }

    public ChatConversation getDirectConversation(String userId) {
        if (this.conversationMap.get(userId) != null && this.conversationMap.get(userId).type == ChatConversation.ConversationType.DIRECT) {
            return this.conversationMap.get(userId);
        } else if (this.connectedPeers.get(userId) != null) {
            ChatConversation conversation = new DirectConversation(this, this.connectedPeers.get(userId), keyPair.getPrivate());
            this.addConversation(userId, conversation);
            return conversation;
        }
        return null;
    }

    public ChatConversation getGroupConversation(String groupId) {
        // TODO: fetching unknown groups from sender
        return this.conversationMap.get(groupId);
    }

    @Override
    public IConversation[] getConversations() {
        return this.conversations.toArray(new ChatConversation[this.conversations.size()]);
    }

    @Override
    public void addConversation(String name, IUser... users) {
        // TODO: support group chats
    }

    @Override
    public IUser[] getNewUsers() {
        return this.availablePeers.values().toArray(new PeerIdentity[this.availablePeers.size()]);
    }

    @Override
    public IUser[] getConnectedUsers() {
        return this.connectedPeers.values().toArray(new PeerInfo[this.connectedPeers.size()]);
    }

    @Override
    public IUser getUserById(String id) {
        return this.connectedPeers.get(id);
    }

    public void sendMessage(IUser user, String message) {
        ChatConversation conversation = this.getDirectConversation(user.getUniqueID());
        conversation.sendMessage(new ChatMessage(this.id, UUID.randomUUID().toString(), user.getUniqueID(), null, ChatMessage.CONTENT_TYPE_TEXT, message));
    }

    @AllArgsConstructor
    public class PeerInfo implements IUser {
        public final String id;
        public final String name;
        @Getter
        public final String address;
        public final Key peerSharedKey;
        public final PublicKey publicKey;
        @Getter
        public final String fingerprint;

        public Date lastUpdateTime;

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getUniqueID() {
            return this.id;
        }

        @Override
        public boolean isConfirmed() {
            return true;
        }

        @Override
        public String toString() {
            return "ID: " + id + "\n" + "Name: " + name + "\n" + "Address: " + address + "\n" + "Fingerprint: " + getFingerprint();
        }

        public void onUpdate() {
            this.lastUpdateTime = new Date();
        }
    }
}
