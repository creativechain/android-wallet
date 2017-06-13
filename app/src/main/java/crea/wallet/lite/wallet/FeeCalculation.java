package crea.wallet.lite.wallet;

import android.util.Log;

import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import com.chip_chap.services.cash.coin.BitCoin;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.InsufficientMoneyException;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionOutput;
import org.creativecoinj.uri.BitcoinURI;
import org.creativecoinj.uri.BitcoinURIParseException;
import org.creativecoinj.wallet.DefaultCoinSelector;
import org.creativecoinj.wallet.SendRequest;
import org.creativecoinj.wallet.Wallet;

import static crea.wallet.lite.application.Constants.WALLET.DONATION_ADDRESS;

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
    private FeeCategory feeCategory;
    private boolean hasSufficientMoney;
    private boolean hasError = false;
    private boolean emptyWallet = false;

    public FeeCalculation(Wallet wallet, Address address, FeeCategory category) {
        this.wallet = wallet;
        this.emptyWallet = true;
        this.address = address;
        this.feeCategory = category;
        try {
            calculate();
        } catch (BitcoinURIParseException e) {
            Log.e(TAG, "Failed to calculate...", e);
        }
    }

    public FeeCalculation(Wallet wallet, FeeCategory category) {
        this(wallet, DONATION_ADDRESS, category);
    }

    public FeeCalculation(Wallet wallet, Coin amountToSent, FeeCategory category) {
        this(wallet, DONATION_ADDRESS, amountToSent, category);
    }

    public FeeCalculation(Wallet wallet, Address address, Coin amountToSent, FeeCategory category) {
        this(wallet, category);
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
            PaymentIntent pi = PaymentIntent.fromBitcoinUri(new BitcoinURI("creativecoin:" + address.toString() + "?amount=" + BitCoin.valueOf(amountToSent.longValue()).getDoubleValue()));
            sReq = pi.toSendRequest();
        }

        Configuration conf = Configuration.getInstance();
        sReq.signInputs = false;
        sReq.feePerKb = conf.getTransactionFee(this.feeCategory);
        try {
            if (conf.isSpendPendintTxAvailable()) {
                wallet.allowSpendingUnconfirmedTransactions();
            } else {
                wallet.setCoinSelector(new DefaultCoinSelector());
            }

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

    public SendRequest getsReq() {
        return sReq;
    }

    public boolean isToDonationAddress() {
        return address.equals(DONATION_ADDRESS);
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return sReq.tx.toString();
    }
}
