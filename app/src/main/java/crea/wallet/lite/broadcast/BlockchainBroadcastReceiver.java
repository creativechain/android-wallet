package crea.wallet.lite.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.creativecoinj.core.Sha256Hash;
import org.creativecoinj.core.Transaction;

import java.util.ArrayList;
import java.util.List;

import crea.wallet.lite.connection.ConnectedPeer;
import crea.wallet.lite.wallet.WalletHelper;

/**
 * Created by ander on 15/11/16.
 */
public abstract class BlockchainBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "TransactionBroadcastReceiver";

    private static final String BASE_ACTION = BlockchainBroadcastReceiver.class.getPackage().getName();

    public static final String PRICE_UPDATE = BASE_ACTION + ".price_update";
    public static final String TRANSACTION_SENT = BASE_ACTION + ".trasaction_sent";
    public static final String TRANSACTION_RECEIVED = BASE_ACTION + ".trasaction_received";
    public static final String LAST_BLOCK_RECEIVED = BASE_ACTION + ".last_block_received";
    public static final String BLOCKCHAIN_RESET = BASE_ACTION + ".blockchain_reset";
    public static final String ACTION_SYNC_STARTED = BASE_ACTION + ".sync_started";
    public static final String ACTION_PEERS_CHANGED = BASE_ACTION + ".peers_changed";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(PRICE_UPDATE)) {
            onPriceUpdated();
        } else if (action.equals(TRANSACTION_SENT)) {
            String txHash = intent.getExtras().getString("txId");
            Transaction t = WalletHelper.INSTANCE.getTransaction(Sha256Hash.wrap(txHash));
            onTransactionSend(t);
        } else if (action.equals(TRANSACTION_RECEIVED)) {
            String txHash = intent.getExtras().getString("txId");
            Transaction t = WalletHelper.INSTANCE.getTransaction(Sha256Hash.wrap(txHash));
            onTransactionSend(t);
        } else if (action.equals(LAST_BLOCK_RECEIVED)) {
            onLastDownloadedBlock();
        } else if (action.equals(BLOCKCHAIN_RESET)) {
            onBlockChainReset();
        } else if (action.equals(ACTION_SYNC_STARTED)) {
            onSyncStarted();
        } else if (action.equals(ACTION_PEERS_CHANGED)) {
            List<ConnectedPeer> peers = (List<ConnectedPeer>) intent.getExtras().getSerializable("peers");
            onPeers(peers);
        }
    }

    public void onPriceUpdated() {

    }

    public void onBlockChainReset() {

    }

    public void onTransactionSend(Transaction transaction) {

    }

    public void onTransactionReceived(Transaction transaction) {

    }

    public void onLastDownloadedBlock() {

    }

    public void onSyncStarted() {

    }

    public void onPeers(List<ConnectedPeer> peers) {

    }
}
