package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
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
    private ConnectedPeerAdapter adapter;
    private View noPeers;
    private Handler peerHandler;

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

        adapter = new ConnectedPeerAdapter(this, new ArrayList<ConnectedPeer>());
        list.setAdapter(adapter);
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

        adapter.replace(peerList);
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

    private void setUpHandler() {
        if (peerHandler == null) {
            peerHandler = new Handler();
        }
    }

    private void startHandler() {
        setUpHandler();
        peerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getPeerList();
                peerHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void stopHandler() {
        peerHandler.removeCallbacksAndMessages(null);
        peerHandler = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPeerList();
        registerReceiver(BLOCKCHAIN_RECEIVER, new IntentFilter(BlockchainBroadcastReceiver.ACTION_PEERS_CHANGED));
        startHandler();

    }

    @Override
    protected void onPause() {
        unregisterReceiver(BLOCKCHAIN_RECEIVER);
        stopHandler();
        super.onPause();
    }
}
