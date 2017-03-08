package crea.wallet.lite.wallet;

import android.util.Log;

import crea.wallet.lite.application.Constants;
import crea.wallet.lite.util.Sha384Hash;
import com.chip_chap.services.user.User;

import org.creacoinj.crypto.MnemonicCode;
import org.creacoinj.crypto.MnemonicException;
import org.creacoinj.wallet.DeterministicSeed;
import org.creacoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ander on 1/03/16.
 */
public final class WalletGenerator {

    private static final String TAG = "WalletGenerator";

    private HashMap<File, Wallet> wallets;
    private List<String> words;
    private int numOfWallets;
    private long creationTime;
    private boolean isNewAccount = false;
    private boolean forceOverride = false;


    public WalletGenerator(List<String> words, long creationTime) {
        this.wallets = new HashMap<>();
        this.words = words;
        this.creationTime = creationTime / 1000;
    }

    public WalletGenerator(DeterministicSeed seed) {
        this(seed.getMnemonicCode(), seed.getCreationTimeSeconds() * 1000);
    }

    public WalletGenerator setNumOfWallets(int numOfWallets) {
        this.numOfWallets = numOfWallets;
        return this;
    }

    public WalletGenerator setIsNewAccount(boolean isNewAccount) {
        this.isNewAccount = isNewAccount;
        return this;
    }

    public WalletGenerator setForceOverride(boolean forceOverride) {
        this.forceOverride = forceOverride;
        return this;
    }

    public HashMap<File, Wallet> getWallets() {
        return wallets;
    }

    public WalletGenerator create(int num, CharSequence encryptionKey) throws MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        File f = fileFrom(num+1);

        if (!f.exists() || User.getCurrentUser() == null || forceOverride) {
            DeterministicSeed seed;
            if (num <= 1) {
                seed = new DeterministicSeed(words, null, "", creationTime);
            } else {
                seed = new DeterministicSeed(words, derivate(num), "", creationTime);
            }

            Log.e(TAG, "Seed " + (num+1) + ": " + seed.toString());
            Wallet w = Wallet.fromSeed(Constants.WALLET.NETWORK_PARAMETERS, seed);

            if (encryptionKey != null && !encryptionKey.toString().isEmpty()) {
                Log.e(TAG, "Encrypting wallet with '" + encryptionKey + "'");
                w.encrypt(encryptionKey);
            }
            wallets.put(f, w);
        }

        return this;
    }

    public WalletGenerator create(int num) throws MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        return create(num, null);
    }

    public WalletGenerator create(CharSequence encryptionKey) throws MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        for (int x = 0; x < numOfWallets; x++) {
            create(x, encryptionKey);
        }

        return this;
    }

    public WalletGenerator create() throws MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        return create(null);
    }

    private File backupFileFrom(int num) {
        if (num == 1) {
            return Constants.WALLET.MAIN_WALLET_BACKUP_FILE;
        }

        return new File(Constants.WALLET.WALLET_BACKUP_FILES_NAME + num + Constants.FILES.FILENAME_NETWORK_SUFFIX);
    }

    private File fileFrom(int num) {
        if (num == 1) {
            return Constants.WALLET.FIRST_WALLET_FILE;
        }

        return new File(Constants.WALLET.WALLET_FILES_NAME + num + Constants.FILES.FILENAME_NETWORK_SUFFIX);
    }

    private byte[] derivate(int derivations) throws MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        derivations = derivations -1;
        byte[] derived = MnemonicCode.INSTANCE.toEntropy(words);

        for (int x = 0; x < derivations; x++) {
            derived = Sha384Hash.hash(derived);
        }

        Sha384Hash sha384Hash = Sha384Hash.of(derived);
        Log.e(TAG, "Sha384Hash: " + sha384Hash.toString());

        return derived;
    }

    public void saveInFiles() {
        File folder = new File(Constants.WALLET.WALLET_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (File wf : wallets.keySet()) {
            Wallet w = wallets.get(wf);

            if (forceOverride) {
                try {
                    w.saveToFile(wf);
                } catch (IOException e) {
                    Log.e(TAG, "Impossible save the wallet", e);
                }
            } else {
                Log.e(TAG, "File " + wf.getAbsolutePath() + " already exist. New wallet not saved.");
            }
        }
    }
}
