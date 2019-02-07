package crea.wallet.lite.util.wrapper;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionOutput;

import java.util.ArrayList;
import java.util.List;

import crea.wallet.lite.db.BookAddress;

import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;
import static crea.wallet.lite.wallet.WalletHelper.INSTANCE;

/**
 * Created by Andersson G. Acosta on 15/06/17.
 */

public class TxInfo {

    private static final String TAG = "TxInfo";
    private Transaction tx;

    private Coin fee = Coin.ZERO;
    private Coin amountSend = Coin.ZERO;
    private Coin amountReceived = Coin.ZERO;
    private List<Address> inputAddresses = new ArrayList<>();
    private List<Address> outputAddresses = new ArrayList<>();
    private boolean sentFromUser;

    public TxInfo(@NonNull Transaction tx) {
        this.tx = tx;
        extractInfo();
    }

    private void extractInfo() {
        fee = tx.getFee() != null ? tx.getFee() : Coin.ZERO;
        Coin sended = tx.getValueSentFromMe(INSTANCE.getWallet());
        sentFromUser = sended.isGreaterThan(Coin.ZERO);

        List<TransactionOutput> outputs = tx.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            for (TransactionOutput out : outputs) {
                if (out.isMine(INSTANCE.getWallet())) {
                    if (!sentFromUser) {
                        //Received
                        amountReceived = amountReceived.add(out.getValue());
                        try {
                            outputAddresses.add(out.getScriptPubKey().getToAddress(NETWORK_PARAMETERS, true));
                        } catch (Exception ignored) {
                            Log.e(TAG, "Fail to get Address", ignored);
                        }
                    }
                } else if (sentFromUser) {
                    //Send
                    amountSend = amountSend.add(out.getValue());
                    try {
                        outputAddresses.add(out.getScriptPubKey().getToAddress(NETWORK_PARAMETERS, true));
                    } catch (Exception ignored) {}
                }
            }
        }

    }

    public String getHashAsString() {
        return tx.getHashAsString();
    }

    public byte[] getRaw() {
        return tx.unsafeBitcoinSerialize();
    }

    public boolean isSentFromUser() {
        return sentFromUser;
    }

    public boolean isConfirmed() {
        return !tx.isPending();
    }

    public boolean isPayToManyTransaction() {
        return tx.getOutputs().size() > 20;
    }

    public boolean isReplaceable() {
        return isSentFromUser() && !isConfirmed() && !isPayToManyTransaction();
    }

    public boolean isRBFTransaction() {
        return getAddressesResolved().isEmpty();
    }

    public long getTime() {
        return tx.getUpdateTime().getTime();
    }

    public int getConfirmations() {
        return tx.getConfidence().getDepthInBlocks();
    }

    public Coin getFee() {
        return fee;
    }

    public Coin getAmountSend() {
        return amountSend;
    }

    public Coin getAmountReceived() {
        return amountReceived;
    }

    public Coin getTransactionedCoin() {
        return sentFromUser ? getAmountSend() : getAmountReceived();
    }

    public List<Address> getInputAddresses() {
        return inputAddresses;
    }

    public List<Address> getOutputAddresses() {
        return outputAddresses;
    }

    private List<String> getAddressesResolved(List<Address> addresses) {
        List<String> resolvedAddresses = new ArrayList<>();

        for (Address a : addresses) {
            BookAddress resolved = BookAddress.resolveAddress(a);
            String label;
            if (resolved != null) {
                label = TextUtils.isEmpty(resolved.getLabel()) ? a.toBase58() : resolved.getLabel();
            } else {
                label = a.toBase58();
            }
            resolvedAddresses.add(label);
        }

        return resolvedAddresses;
    }

    public List<String> getInputAddressesResolved() {
        return getAddressesResolved(getInputAddresses());
    }

    public Transaction getTx() {
        return tx;
    }

    public List<String> getOutputAddressesResolved() {
        return getAddressesResolved(getOutputAddresses());
    }

    public List<String> getAddressesResolved() {
        return getOutputAddressesResolved();
    }


}
