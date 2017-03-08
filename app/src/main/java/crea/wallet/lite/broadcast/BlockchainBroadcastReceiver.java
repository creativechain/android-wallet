package crea.wallet.lite.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.activeandroid.query.Select;
import com.chip_chap.services.transaction.Btc2BtcTransaction;

/**
 * Created by ander on 15/11/16.
 */
public abstract class BlockchainBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "TransactionBroadcastReceiver";

    private static final String BASE_ACTION = BlockchainBroadcastReceiver.class.getPackage().getName();

    public static final String TRANSACTION_SENT = BASE_ACTION + ".trasaction_sent";
    public static final String TRANSACTION_RECEIVED = BASE_ACTION + ".trasaction_received";
    public static final String LAST_BLOCK_RECEIVED = BASE_ACTION + ".last_block_received";
    public static final String BLOCKCHAIN_RESET = BASE_ACTION + ".blockchain_reset";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(TRANSACTION_SENT)) {
            long id = intent.getExtras().getLong("txId");
            Btc2BtcTransaction t = findBtcTransaction(id);
            onTransactionSend(t);
        } else if (action.equals(TRANSACTION_RECEIVED)) {
            long id = intent.getExtras().getLong("txId");
            Btc2BtcTransaction t = findBtcTransaction(id);
            onTransactionSend(t);
        } else if (action.equals(LAST_BLOCK_RECEIVED)) {
            onLastDownloadedBlock();
        } else if (action.equals(BLOCKCHAIN_RESET)) {
            onBlockChainReset();
        }
    }

    public void onBlockChainReset() {

    }

    private Btc2BtcTransaction findBtcTransaction(long id) {
        return new Select().from(Btc2BtcTransaction.class).where("id = " + id).executeSingle();
    }

    public void onTransactionSend(Btc2BtcTransaction transaction) {

    }

    public void onTransactionReceived(Btc2BtcTransaction transaction) {

    }

    public void onLastDownloadedBlock() {

    }
}
