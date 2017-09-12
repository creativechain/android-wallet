package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import crea.wallet.lite.R;
import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.connection.ConnectedPeer;
import crea.wallet.lite.service.CreativeCoinService;
import crea.wallet.lite.ui.adapter.ConnectedPeerAdapter;

/**
 * Created by Andersson G. Acosta on 1/09/17.
 */

public class PeerListActivity extends AppCompatActivity {

    private static final String TAG = "NodeListActivity";

    public final BlockchainBroadcastReceiver BLOCKCHAIN_RECEIVER = new BlockchainBroadcastReceiver() {

        @Override
        public void onPeers(List<ConnectedPeer> peers) {
            setupPeerList(peers);
        }
    };

    private RecyclerView list;
    private View noPeers;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_list);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        noPeers = findViewById(R.id.no_peers);
        list = (RecyclerView) findViewById(R.id.peer_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);
    }

    private void getPeerList() {
        Intent intent = new Intent(this, CreativeCoinService.class);
        intent.setAction(CreativeCoinService.ACTION_SEND_PEERS);
        startService(intent);
    }

    private void setupPeerList(List<ConnectedPeer> peerList) {
        Log.e(TAG, "Received " + peerList.size() + " peers");
        noPeers.setVisibility(peerList.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(peerList.isEmpty() ? View.GONE : View.VISIBLE);

        ConnectedPeerAdapter adapter = new ConnectedPeerAdapter(this, peerList);
        list.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPeerList();
        registerReceiver(BLOCKCHAIN_RECEIVER, new IntentFilter(BlockchainBroadcastReceiver.ACTION_PEERS_CHANGED));
    }

    @Override
    protected void onPause() {
        unregisterReceiver(BLOCKCHAIN_RECEIVER);
        super.onPause();
    }
}
