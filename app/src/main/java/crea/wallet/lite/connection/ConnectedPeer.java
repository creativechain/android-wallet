package crea.wallet.lite.connection;

import org.creativecoinj.core.Peer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andersson G. Acosta on 1/09/17.
 */

public class ConnectedPeer implements Serializable {

    private static final long serialVersionUID = -6152788684692053611L;

    private String host;
    private String client;
    private long protocol;
    private long blocks;
    private long pingTime;

    public ConnectedPeer(String host, String client, long protocol, long blocks, long pingTime) {
        this.host = host;
        this.client = client;
        this.protocol = protocol;
        this.blocks = blocks;
        this.pingTime = pingTime;
    }

    public ConnectedPeer(Peer peer) {
        this(peer.getAddress().getAddr().getHostAddress() + ":" + peer.getAddress().getPort(),
                peer.getPeerVersionMessage().subVer, peer.getPeerVersionMessage().clientVersion,
                peer.getBestHeight(), peer.getLastPingTime());


    }

    public String getHost() {
        return host;
    }

    public String getClient() {
        return client;
    }

    public long getProtocol() {
        return protocol;
    }

    public long getBlocks() {
        return blocks;
    }

    public long getPingTime() {
        return pingTime;
    }

    public static ArrayList<ConnectedPeer> wrapAsList(Peer... peers) {
        ArrayList<ConnectedPeer> connectedPeers = new ArrayList<>();

        for (Peer p : peers) {
            connectedPeers.add(new ConnectedPeer(p));
        }

        return connectedPeers;
    }

    public static ArrayList<ConnectedPeer> wrapAsList(List<Peer> peers) {
        ArrayList<ConnectedPeer> connectedPeers = new ArrayList<>();

        for (Peer p : peers) {
            connectedPeers.add(new ConnectedPeer(p));
        }

        return connectedPeers;
    }
}
