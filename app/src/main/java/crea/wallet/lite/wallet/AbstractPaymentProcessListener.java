package crea.wallet.lite.wallet;

import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionConfidence;

/**
 * Created by ander on 16/11/16.
 */
public abstract class AbstractPaymentProcessListener implements PaymentProcessListener {

    private static final String TAG = "AbstractPaymentProcessListener";

    @Override
    public void onSuccess(Transaction tx) {

    }

    @Override
    public void onInsufficientMoney(Coin missing) {

    }

    @Override
    public void onInvalidEncryptionKey() {

    }

    @Override
    public void onSigning() {

    }

    @Override
    public void onSending() {

    }

    @Override
    public void onFailure(Exception exception) {

    }

    @Override
    public void onConfidenceChanged(TransactionConfidence confidence, ChangeReason reason) {

    }
}
