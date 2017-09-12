package crea.wallet.lite.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import crea.wallet.lite.R;
import crea.wallet.lite.connection.ConnectedPeer;

/**
 * Created by ander on 16/11/16.
 */
public class ConnectedPeerAdapter extends RecyclerAdapter<ConnectedPeerAdapter.ViewHolder, ConnectedPeer> {

    private static final String TAG = "ConnectedPeerAdapter";


    public ConnectedPeerAdapter(Activity activity, List<ConnectedPeer> peerList) {
        super(activity);
        this.itemList = peerList;
    }

    @Override
    protected List<ConnectedPeer> getItemList() {
        return this.itemList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = getLayoutInflater().inflate(R.layout.peer_row, parent, false);
        return new ViewHolder(convertView);
    }

    @Override
    public void onBindHolder(final ViewHolder holder, int position, ConnectedPeer peer) {

        holder.host.setText(peer.getHost());
        holder.protocol.setText(getString(R.string.protocol_version_dots, peer.getProtocol()));
        holder.blocks.setText(getString(R.string.blocks, peer.getBlocks()));
        holder.ping.setText(getString(R.string.ms, peer.getPingTime()));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView host;
        TextView client;
        TextView protocol;
        TextView blocks;
        TextView ping;

        public ViewHolder(View v) {
            super(v);
            host = (TextView) v.findViewById(R.id.host);
            client = (TextView) v.findViewById(R.id.client);
            protocol = (TextView) v.findViewById(R.id.protocol);
            blocks = (TextView) v.findViewById(R.id.blocks);
            ping = (TextView) v.findViewById(R.id.ping);
        }
    }
}
