package crea.wallet.lite.wallet;

import android.util.Log;

import crea.wallet.lite.application.Constants;

import org.creativecoinj.crypto.MnemonicException;
import org.creativecoinj.wallet.DeterministicSeed;
import org.creativecoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;

import java.util.Date;
import java.util.List;

/**
 * Created by ander on 1/03/16.
 */
public final class WalletGenerator {

    private static final String TAG = "WalletGenerator";

    private Wallet wallet;
    private List<String> words;
    private long creationTime;
    private boolean isNewAccount = false;
    private boolean forceOverride = false;


    public WalletGenerator(List<String> words, long creationTime) {
        this.words = words;
        this.creationTime = creationTime / 1000;
    }

    public WalletGenerator(DeterministicSeed seed) {
        this(seed.getMnemonicCode(), seed.getCreationTimeSeconds() * 1000);
    }

    public WalletGenerator setIsNewAccount(boolean isNewAccount) {
        this.isNewAccount = isNewAccount;
        return this;
    }

    public WalletGenerator setForceOverride(boolean forceOverride) {
        this.forceOverride = forceOverride;
        return this;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public WalletGenerator create() throws MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        return create(null);
    }

    public WalletGenerator create(CharSequence encryptionKey) throws MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        File f = Constants.WALLET.FIRST_WALLET_FILE;
        if (!f.exists() || forceOverride) {
            DeterministicSeed seed;

            seed = new DeterministicSeed(words, null, "", creationTime);

            Log.e(TAG, "Created seed date " + new Date(seed.getCreationTimeSeconds() * 1000).toLocaleString());
            wallet = Wallet.fromSeed(Constants.WALLET.NETWORK_PARAMETERS, seed);

            if (encryptionKey != null && !encryptionKey.toString().isEmpty()) {
                Log.e(TAG, "Encrypting wallet with '" + encryptionKey + "'");
                wallet.encrypt(encryptionKey);
            }

        }

        return this;
    }


    public void saveInFiles() {
        File folder = new File(Constants.WALLET.WALLET_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File wf = Constants.WALLET.FIRST_WALLET_FILE;
        if (forceOverride) {
            try {
                wallet.saveToFile(wf);
            } catch (IOException e) {
                Log.e(TAG, "Impossible save the wallet", e);
            }
        } else {
            Log.e(TAG, "File " + wf.getAbsolutePath() + " already exist. New wallet not saved.");
        }
    }
}
