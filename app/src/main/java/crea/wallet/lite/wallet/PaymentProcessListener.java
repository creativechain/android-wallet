package crea.wallet.lite.wallet;

import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionConfidence;

/**
 * Created by ander on 16/11/16.
 */
public interface PaymentProcessListener extends TransactionConfidence.Listener{

    void onSuccess(Transaction tx);
    void onRejected(Transaction tx);
    void onInsufficientMoney(Coin missing);
    void onInvalidEncryptionKey();
    void onSigning();
    void onSending();
    void onFailure(Exception exception);
}
