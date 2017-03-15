package crea.wallet.lite.wallet;

import android.util.Log;

import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import com.chip_chap.services.cash.coin.BitCoin;

import org.creacoinj.core.Address;
import org.creacoinj.core.Coin;
import org.creacoinj.core.InsufficientMoneyException;
import org.creacoinj.core.Transaction;
import org.creacoinj.core.TransactionOutput;
import org.creacoinj.uri.BitcoinURI;
import org.creacoinj.uri.BitcoinURIParseException;
import org.creacoinj.wallet.SendRequest;
import org.creacoinj.wallet.Wallet;

/**
 * Created by ander on 16/09/15.
 */
public class FeeCalculation {

    private static final String TAG = "FeeCalculation";

    private Wallet wallet;
    private Coin amountToSent;
    private Address address;
    private SendRequest sReq;
    private Coin totalToSent = Coin.ZERO;
    private Coin missing = Coin.ZERO;
    private Coin fee = Coin.ZERO;
    private boolean hasSufficientMoney;
    private boolean hasError = false;
    private boolean emptyWallet = false;

    public FeeCalculation(Wallet wallet, Address address) {
        this.wallet = wallet;
        this.emptyWallet = true;
        this.address = address;
        try {
            calculate();
        } catch (BitcoinURIParseException e) {
            Log.e(TAG, "Failed to calculate...", e);
        }
    }

    public FeeCalculation(Wallet wallet) {
        this(wallet, Constants.WALLET.DONATION_ADDRESS);
    }

    public FeeCalculation(Wallet wallet, Coin amountToSent) {
        this(wallet, Constants.WALLET.DONATION_ADDRESS, amountToSent);
    }

    public FeeCalculation(Wallet wallet, Coin amountToSent, Address... inputs) {
        Transaction tx = new Transaction(wallet.getParams());
        TransactionOutput txO = new TransactionOutput(wallet.getParams(), tx, amountToSent, Constants.WALLET.DONATION_ADDRESS);
        tx.addInput(txO);
    }

    public FeeCalculation(Wallet wallet, Address address, Coin amountToSent) {
        this(wallet);
        this.emptyWallet = false;
        this.address = address;
        this.amountToSent = amountToSent;
        try {
            calculate();
        } catch (BitcoinURIParseException e) {
            Log.e(TAG, "Failed to calculate...", e);
        }
    }

    private void calculate() throws BitcoinURIParseException {
        if (emptyWallet) {
            sReq = SendRequest.emptyWallet(address);
        } else {
            PaymentIntent pi = PaymentIntent.fromBitcoinUri(new BitcoinURI("creacoin:" + address.toString() + "?amount=" + BitCoin.valueOf(amountToSent.longValue()).getDoubleValue()));
            sReq = pi.toSendRequest();
        }

        sReq.signInputs = false;
        sReq.feePerKb = Configuration.getInstance().getTransactionFee();
        try {
            wallet.completeTx(sReq);
            hasError = false;
            hasSufficientMoney = true;
        } catch (InsufficientMoneyException e) {
            hasSufficientMoney = false;
            missing = e.missing;
            e.printStackTrace();
        } catch (Wallet.CompletionException e) {
            e.printStackTrace();
            hasError = true;
            //swallow
        }

        Log.i(TAG, sReq.tx.toString());

        fee = sReq.tx.getFee();
        totalToSent = sReq.tx.getValueSentFromMe(wallet).subtract(fee);

    }

    public Coin getTxFee() {
        return fee;
    }

    public Coin getTotalToSent() {
        return totalToSent;
    }

    public boolean hasSufficientMoney() {
        return hasSufficientMoney;
    }

    public Coin getMissing() {
        return missing;
    }

    public boolean hasError() {
        return hasError;
    }

    @Override
    public String toString() {
        return sReq.tx.toString();
    }
}
