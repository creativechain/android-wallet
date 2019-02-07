package crea.wallet.lite.ui.main;

import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionConfidence;
import org.creativecoinj.wallet.Wallet;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.ui.tool.State;
import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.wallet.AbstractPaymentProcessListener;
import crea.wallet.lite.wallet.FeeCategory;
import crea.wallet.lite.wallet.PaymentProcessListener;
import crea.wallet.lite.wallet.WalletHelper;

import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.TRANSACTION_RECEIVED;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.TRANSACTION_REJECTED;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.TRANSACTION_SENT;

/**
 * Created by Andersson G. Acosta on 19/07/17.
 */

public abstract class PrepareTxActivity extends AppCompatActivity {

    private final BlockchainBroadcastReceiver TRANSACTION_RECEIVER = new BlockchainBroadcastReceiver() {
        @Override
        public void onTransactionSend(Transaction transaction) {
            wallet = WalletHelper.INSTANCE.getWallet();
            CONFIDENCE_LISTENER.onSuccess(transaction);
        }

        @Override
        public void onTransactionReceived(Transaction transaction) {
            wallet = WalletHelper.INSTANCE.getWallet();
        }

        @Override
        public void onTransactionRejected(Transaction transaction) {
            CONFIDENCE_LISTENER.onRejected(transaction);
        }
    };

    protected final PaymentProcessListener CONFIDENCE_LISTENER = new AbstractPaymentProcessListener() {
        @Override
        public void onSigning() {
            onTransactionState(State.SIGNING);
        }

        @Override
        public void onSending() {
            onTransactionState(State.SENDING);
        }

        @Override
        public void onSuccess(Transaction tx) {
            PrepareTxActivity.this.tx = tx;
            onTransactionState(State.SENT);
        }

        @Override
        public void onRejected(Transaction tx) {
            PrepareTxActivity.this.tx = tx;
            onTransactionState(State.REJECTED);
        }

        @Override
        public void onInsufficientMoney(Coin missing) {
            State result = State.FAILED;
            result.setExplRes(getString(R.string.insufficient_money_text, missing.toFriendlyString()));
            onTransactionState(result);
        }

        @Override
        public void onInvalidEncryptionKey() {
            State result = State.FAILED;
            result.setExplRes(getString(R.string.invalid_encryption_key));
            onTransactionState(result);
        }

        @Override
        public void onFailure(Exception exception) {
            State result = State.FAILED;
            result.setExplRes(exception.getLocalizedMessage());
            onTransactionState(result);
        }

        @Override
        public void onConfidenceChanged(TransactionConfidence confidence, ChangeReason reason) {
            TransactionConfidence.ConfidenceType type = confidence.getConfidenceType();
            int peers = confidence.numBroadcastPeers();
            State result = State.FAILED;
            switch (type) {
                case BUILDING:
                    if (peers > 1) {
                        result = State.SENT;
                    } else {
                        result = State.SENDING;
                        result.setExplRes(getString(R.string.broadcasting_transaction_in_nodes, peers));
                    }
                    break;
                case PENDING:
                    result = State.SENDING;
                    result.setExplRes(getString(R.string.broadcasting_transaction_in_nodes, peers));
                    break;
                case DEAD:
                    result = State.FAILED;
                    result.setExplRes(getString(R.string.already_spent_funds));
                    break;

            }

            onTransactionState(result);
        }
    };
    protected FeeCategory feeCategory;
    protected Wallet wallet;
    protected Configuration conf;
    protected Transaction tx;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conf = Configuration.getInstance();
        wallet = WalletHelper.INSTANCE.getWallet();
        feeCategory = conf.getFeeCategory();

    }

    public abstract void onTransactionState(State state);

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(TRANSACTION_RECEIVER);
        super.onPause();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(TRANSACTION_SENT);
        filter.addAction(TRANSACTION_RECEIVED);
        filter.addAction(TRANSACTION_REJECTED);
        registerReceiver(TRANSACTION_RECEIVER, filter);
    }

    @Override
    protected void onDestroy() {
        DialogFactory.removeDialogsFrom(getClass());
        super.onDestroy();
    }
}
