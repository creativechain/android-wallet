package crea.wallet.lite.service;

import org.creacoinj.core.ECKey;
import org.creacoinj.core.Transaction;
import org.creacoinj.script.Script;
import org.creacoinj.wallet.Wallet;
import org.creacoinj.wallet.listeners.WalletEventListener;

import java.util.List;

/**
 * Created by ander on 4/04/16.
 */
public abstract class AbstractWalletCoinListener implements WalletEventListener {

    private static final String TAG = "AbstractWalletCoinListener";

    @Override
    public void onReorganize(Wallet wallet) {

    }

    @Override
    public void onTransactionConfidenceChanged(Wallet wallet, Transaction transaction) {

    }

    @Override
    public void onWalletChanged(Wallet wallet) {

    }

    @Override
    public void onScriptsChanged(Wallet wallet, List<Script> list, boolean b) {

    }

    @Override
    public void onKeysAdded(List<ECKey> list) {

    }
}
