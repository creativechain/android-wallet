package crea.wallet.lite.wallet;

import org.creacoinj.core.Coin;
import org.creacoinj.core.Transaction;
import org.creacoinj.core.TransactionConfidence;

/**
 * Created by ander on 16/11/16.
 */
public interface PaymentProcessListener extends TransactionConfidence.Listener{

    void onSuccess(Transaction tx);
    void onInsufficientMoney(Coin missing);
    void onInvalidEncryptionKey();
    void onSigning();
    void onSending();
    void onFailure(Exception exception);
}
