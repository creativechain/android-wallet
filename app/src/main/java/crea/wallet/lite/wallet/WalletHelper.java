package crea.wallet.lite.wallet;

import android.support.annotation.NonNull;
import android.util.Log;

import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.util.Io;
import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.coin.BitCoin;
import com.chip_chap.services.user.WalletBalance;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.Block;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Context;
import org.creativecoinj.core.InsufficientMoneyException;
import org.creativecoinj.core.NetworkParameters;
import org.creativecoinj.core.Sha256Hash;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.crypto.MnemonicException;
import org.creativecoinj.wallet.CoinSelector;
import org.creativecoinj.wallet.DeterministicSeed;
import org.creativecoinj.wallet.Protos;
import org.creativecoinj.wallet.SendRequest;
import org.creativecoinj.wallet.UnreadableWalletException;
import org.creativecoinj.wallet.Wallet;
import org.creativecoinj.wallet.WalletFiles;
import org.creativecoinj.wallet.WalletProtobufSerializer;
import org.creativecoinj.wallet.listeners.WalletEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static crea.wallet.lite.application.Constants.WALLET.CONTEXT;

/**
 * Created by ander on 17/09/15.
 */
public class WalletHelper {

    private static final String TAG = "WalletHelper";
    public static final NetworkParameters WALLET_PARAMS = Constants.WALLET.NETWORK_PARAMETERS;
    public static final String WALLET_PATH = Constants.WALLET.WALLET_PATH;

    private static final WalletFiles.Listener AUTOSAVE_LISTENER = new WalletFiles.Listener() {

        @Override
        public void onBeforeAutoSave(File file) {

        }

        @Override
        public void onAfterAutoSave(File file) {
            Log.e(TAG, "Auto saving wallet " + file.getAbsolutePath());
            //Io.chmod(file, 0777);
        }
    };

    public static final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            new Thread(runnable).start();
        }
    };


    public static WalletHelper INSTANCE;
    private boolean newWallet = false;
    private HashMap<File, Wallet> wallets;

    private WalletHelper(HashMap<File, Wallet> wallets) {
        this.wallets = wallets;
        if (wallets.size() <= 0) {
            throw new IllegalArgumentException("The list of wallets should contain at least one object.");
        }
    }

    public boolean isWalletEncrypted(File f) {
        return wallets.get(f).isEncrypted();
    }

    public boolean isWalletEncrypted() {
        return isWalletEncrypted(Configuration.getInstance().getMainWalletFile());
    }

    public File[] getWalletFiles() {
        return wallets.keySet().toArray(new File[1]);
    }

    public void addEventListener(WalletEventListener listener) {
        addEventListener(listener, null);
    }

    public void addEventListener(WalletEventListener listener, Executor executor) {
        if (executor == null) {
            executor = EXECUTOR;
        }
        for (File k : wallets.keySet()) {
            wallets.get(k).addEventListener(listener, executor);
        }

    }

    public void removeEventListener(WalletEventListener listener) {
        for (File k : wallets.keySet()) {
            wallets.get(k).removeEventListener(listener);
        }
    }

    public Address currentMainReceiveAddress() {
        return currentReceiveAddress(Configuration.getInstance().getMainWalletFile());
    }

    public Address currentReceiveAddress(File f) {
        return wallets.get(f).currentReceiveAddress();
    }

    public void addIssuedAddressesToWatch() {
        for (File f : wallets.keySet()) {
            addIssuedAddressesToWatch(f);
        }
    }

    public int addIssuedAddressesToWatch(File f) {
        return addWatchedAddresses(f, getMainReceiveAddresses());
    }

    public int addIssuedMainAddressesToWatch() {
        return addIssuedAddressesToWatch(Configuration.getInstance().getMainWalletFile());
    }

    public int addWatchedAddresses(File f, List<Address> addresses) {
        return wallets.get(f).addWatchedAddresses(addresses, 0);
    }

    public List<Address> getReceiveAddresses(File f) {
        List<Address> list = wallets.get(f).getIssuedReceiveAddresses();
        if (list.size() == 0) {
            list.add(wallets.get(f).freshReceiveAddress());
        }

        return list;
    }

    public List<Address> getMainReceiveAddresses() {
        return getReceiveAddresses(Configuration.getInstance().getMainWalletFile());
    }

    public Coin getTotalBalance() {
        Coin balance = Coin.ZERO;
        for (Wallet w : wallets.values()) {
            balance = balance.add(w.getBalance());
        }

        return balance;
    }

    public Coin getTotalBalance(Wallet.BalanceType balanceType) {
        Coin balance = Coin.ZERO;
        for (Wallet w : wallets.values()) {
            balance = balance.add(w.getBalance(balanceType));
        }

        return balance;
    }

    public Coin getBalance(File f) {
        return wallets.get(f).getBalance();
    }

    public Coin getMainBalance() {
        return getBalance(Configuration.getInstance().getMainWalletFile());
    }

    public WalletBalance getTotalWalletBalance() {
        Coin cBalance = getTotalBalance();
        Coin totalBalance = getTotalBalance(Wallet.BalanceType.ESTIMATED);
        Coin pending = totalBalance.subtract(cBalance);

        Log.i(TAG, "available balance: " + cBalance.getValue() + ", pending balance: " + pending.getValue());

        WalletBalance wb = new WalletBalance();
        wb.setCurrency(Currency.BTC);
        wb.setAvailable(BitCoin.valueOf(cBalance.longValue()).getDoubleValue());
        wb.setBalance(BitCoin.valueOf(cBalance.add(pending).longValue()).getDoubleValue());
        wb.setScale(8);
        wb.setExchange(1);
        return wb;
    }

    public Coin getValueSentToMe(Transaction tx) {
        Coin value = Coin.ZERO;
        for (Wallet w : getWallets()) {
            value = value.add(tx.getValueSentToMe(w));
        }

        return value;
    }

    public Coin getValueSentFromMe(Transaction tx) {
        Coin value = Coin.ZERO;
        for (Wallet w : getWallets()) {
            value = value.add(tx.getValueSentFromMe(w));
        }

        return value;
    }

    public Coin getBalance(File f, CoinSelector coinSelector) {
        return wallets.get(f).getBalance(coinSelector);
    }

    public Coin getMainBalance(CoinSelector coinSelector) {
        return getBalance(Configuration.getInstance().getMainWalletFile(), coinSelector);
    }

    public Coin getMainAddressBalance(File f, Address address) {
        return getBalance(f, new AddressBalance(address));
    }

    public Coin getMainAddressBalance(Address address) {
        return getMainAddressBalance(Configuration.getInstance().getMainWalletFile(), address);
    }

    public Coin getAddressBalance(File f, String address) {
        return getMainAddressBalance(f, Address.fromBase58(Constants.WALLET.NETWORK_PARAMETERS, address));
    }

    public Coin getMainAddressBalance(String address) {
        return getAddressBalance(Configuration.getInstance().getMainWalletFile(), address);
    }

    public void singTransaction(final File f, final CharSequence tryKey, SendRequest sReq) {
        try {
            Context.propagate(CONTEXT);
            Wallet w = wallets.get(f);
            w.decrypt(tryKey);
            w.signTransaction(sReq);
            new Thread() {
                @Override
                public void run() {
                    Context.propagate(CONTEXT);
                    encrypt(f, tryKey);
                }
            }.start();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public SendRequest prepareTransaction(final File f, final CharSequence tryKey, final CharSequence newKey, Coin coinsToSent, Address address, boolean emptyWallet) throws InsufficientMoneyException {

        try {
            wallets.get(f).decrypt(tryKey);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        SendRequest sReq = prepareTransaction(f, coinsToSent, address, emptyWallet);
        new Thread() {
            @Override
            public void run() {
                Context.propagate(CONTEXT);
                encrypt(f, newKey);
            }
        }.start();
        return sReq;
    }

    public SendRequest prepareTransaction(File f, Coin coinsToSent, Address address, boolean emptyWallet) throws InsufficientMoneyException {
        Context.propagate(CONTEXT);
        SendRequest sReq;
        if (emptyWallet) {
            sReq = SendRequest.emptyWallet(address);
        } else {
            sReq = SendRequest.to(address, coinsToSent);
        }

        sReq.feePerKb = Configuration.getInstance().getTransactionFee();
        wallets.get(f).completeTx(sReq);
        return sReq;
    }

    public boolean isNewWallet() {
        return newWallet;
    }

    public void setNewWallet(boolean isNewWallet) {
        this.newWallet = isNewWallet;
    }

    public void commitTx(File f, Transaction tx) {
        wallets.get(f).commitTx(tx);
    }

    public void commitTx(File f, SendRequest sReq) {
        commitTx(f, sReq.tx);
    }

    public Transaction getTransaction(Sha256Hash hash) {
        for (Wallet w : wallets.values()) {
            Transaction t = w.getTransaction(hash);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public boolean isTransactionRelevant(Transaction tx) {
        boolean relevant = false;
        for (Wallet w : wallets.values()) {
            relevant = relevant || w.isTransactionRelevant(tx);
        }

        return relevant;
    }

    public void receivePending(Transaction tx, List<Transaction> dependecies) {
        try {
            for (File f : wallets.keySet()) {
                receivePending(f, tx, dependecies);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receivePending(File f, Transaction tx, List<Transaction> dependencies) {
        wallets.get(f).receivePending(tx, dependencies);
    }

    public void setLastBlock(Block block, int height) {
        for (Wallet w : wallets.values()) {
            w.setLastBlockSeenHash(block.getHash());
            w.setLastBlockSeenHeight(height);
            w.setLastBlockSeenTimeSecs(block.getTimeSeconds());
        }
    }

    public int getLastBlockHeight() {
        return wallets.get(Configuration.getInstance().getMainWalletFile()).getLastBlockSeenHeight();
    }

    public long getFirstKeyCreationTime() {
        return getKeyCreationTime(getWalletFile(1));
    }

    public long getKeyCreationTime(File f) {
        return wallets.get(f).getEarliestKeyCreationTime();
    }

    public DeterministicSeed getKeyChainSeed(CharSequence key) {
        decrypt(Configuration.getInstance().getMainWalletFile(), key);
        DeterministicSeed seed = wallets.get(Configuration.getInstance().getMainWalletFile()).getKeyChainSeed();
        encrypt(Configuration.getInstance().getMainWalletFile(), key);
        return seed;
    }

    public DeterministicSeed getKeyChainSeed(File f) {
        return wallets.get(f).getKeyChainSeed();
    }

    public DeterministicSeed getKeyChainSeed() {
        return wallets.get(Configuration.getInstance().getMainWalletFile()).getKeyChainSeed();
    }

    public int getWalletCount() {
        return getWallets().length;
    }

    public Wallet[] getWallets() {
        return wallets.values().toArray(new Wallet[1]);
    }

    public File getWalletFile(int num) {
        if (num == 1) {
            return Constants.WALLET.FIRST_WALLET_FILE;
        }

        return new File(Constants.WALLET.WALLET_FILES_NAME + num + Constants.FILES.FILENAME_NETWORK_SUFFIX);
    }

    public Wallet getMainWallet() {
        return wallets.get(Configuration.getInstance().getMainWalletFile());
    }

    public Wallet getWallet(int num) {
        return getWallet(getWalletFile(num));
    }

    public Wallet getWallet(File f) {
        return wallets.get(f);
    }

    public void encrypt(CharSequence key) {
        for (File f : wallets.keySet()) {
            encrypt(f, key);
        }
    }

    public void encrypt(File f, CharSequence key) {
        if (wallets.get(f).isEncrypted()) {
            Log.e(TAG, "Wallet " + f. getAbsolutePath() + " is already encrypted.");
            return;
        }
        wallets.get(f).encrypt(key);
    }

    public void decryptAll(CharSequence key) {
        for (File f : wallets.keySet()) {
            decrypt(f, key);
        }
    }

    public void decrypt(CharSequence key) {
        decrypt(Configuration.getInstance().getMainWalletFile(), key);
    }

    private void decrypt(File f, CharSequence key) {
        Context.propagate(CONTEXT);
        if (wallets.get(f).isEncrypted()) {
            wallets.get(f).decrypt(key);
        }
    }

    public void createBackup() {
        for (File f : wallets.keySet()) {
            createBackup(f);
        }
    }

    public void createBackup(File f)	{
        Log.e(TAG, "Creating local backup from " + f.getAbsolutePath());
        long t = System.currentTimeMillis();
        final Protos.Wallet.Builder builder = new WalletProtobufSerializer().walletToProto(wallets.get(f)).toBuilder();

        // strip redundant
        builder.clearTransaction();
        builder.clearLastSeenBlockHash();
        builder.setLastSeenBlockHeight(-1);
        builder.clearLastSeenBlockTimeSecs();

        final Protos.Wallet walletProto = builder.build();
        OutputStream os = null;

        File folder = new File(Constants.APP.BACKUP_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        try	{
            //TODO IMPLEMENTAR NUEVOS ARCHIVOS BACKUP PARA LAS CUENTAS
            os = new FileOutputStream(Constants.WALLET.MAIN_WALLET_BACKUP_FILE);
            walletProto.writeTo(os);
        } catch (final IOException x) {
            Log.e(TAG, "problem writing key backup");
            x.printStackTrace();
        } finally {
            if (os != null) {
                try	{
                    os.close();
                } catch (final IOException x) {
                    // swallow
                }
            }
        }
        t = System.currentTimeMillis() - t;
        Log.e(TAG, "Local backup created in " + t + "ms");
    }

    public void save() {
        File walletFolder = new File(WALLET_PATH);
        if (!walletFolder.exists()) {
            walletFolder.mkdirs();
        }

        for (File f : wallets.keySet()) {
            try {
                wallets.get(f).saveToFile(f);
                Log.e(TAG, "Wallet saved in " + f.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Impossible save the wallet", e);
            }
        }

    }

    public void autoSave(long delayTimeInSeconds) {
        try {
            for (File f : wallets.keySet()) {
                wallets.get(f).autosaveToFile(f, delayTimeInSeconds, TimeUnit.SECONDS, AUTOSAVE_LISTENER);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        Log.e(TAG, "Cleaning wallets...");
        for (File f : wallets.keySet()) {
            wallets.get(f).cleanup();
        }
    }

    public void reset() {
        Log.e(TAG, "Resetting wallets...");
        for (File f : wallets.keySet()) {
            wallets.get(f).reset();
        }
    }

    public boolean isConsistentWallet() {
        return wallets.get(Configuration.getInstance().getMainWalletFile()).isConsistent();
    }

    public void shutdownAutosave() {
        for (Wallet wallet : wallets.values()) {
            try {
                wallet.shutdownAutosaveAndWait();
            } catch (Exception ignore) {
            }
        }
    }

    public NetworkParameters getWalletParams() {
        return WALLET_PARAMS;
    }

    public static boolean isInstanceNull() {
        return INSTANCE == null;
    }

    @Override
    public String toString() {
        return "Wallets=" + getWalletCount() + ", ENCRYPTED=" + isWalletEncrypted();
    }

    private static WalletHelper setAndGetInstance(WalletHelper walletHelper, boolean forceSet) {
        if (isInstanceNull() || forceSet) {
            INSTANCE = walletHelper;
        }

        return INSTANCE;
    }

    public static WalletHelper getInstance() {
        return INSTANCE;
    }

    public static WalletHelper create(List<String> wordList, long creationTime, boolean override) {
        try {
            int numOfWallets = Configuration.getInstance().getHDAccounts();
            Log.e(TAG, "NUM=" + numOfWallets + ", WORDS=" + wordList.size());
            WalletGenerator walletGenerator = new WalletGenerator(wordList, creationTime).setNumOfWallets(numOfWallets).setIsNewAccount(true);
            walletGenerator.setForceOverride(override).create().saveInFiles();
            return setAndGetInstance(new WalletHelper(walletGenerator.getWallets()), true);
        } catch (MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException e) {
            throw new RuntimeException("Failed to create a WalletHelper.", e);
        }

    }

    public static WalletHelper create(boolean override) {
        try {
            return create(WalletApplication.INSTANCE.getMnemonicList(), System.currentTimeMillis(), override);
        } catch (MnemonicException.MnemonicLengthException | IOException e) {
            throw new RuntimeException("Imposible create WalletHelper", e);
        }
    }

    public static WalletHelper fromWallets() {
        HashMap<File, Wallet> wallets = new HashMap<>();
        Wallet mainWallet = null;
        try {
            mainWallet = Wallet.loadFromFile(Configuration.getInstance().getMainWalletFile(), null);
        } catch (UnreadableWalletException e) {
            try {
                mainWallet = loadFromBackup(1);
            } catch (UnreadableWalletException e1) {
                e1.printStackTrace();
            }
        }

        if (mainWallet != null) {
            wallets.put(Configuration.getInstance().getMainWalletFile(), mainWallet);
        }

        for (int x = 2; x <= Constants.WALLET.MAX_HD_ACCOUNTS; x++) {
            File wf = new File(Constants.WALLET.WALLET_FILES_NAME + x + Constants.FILES.FILENAME_NETWORK_SUFFIX);
            if (wf.exists()) {
                try {
                    wallets.put(wf, Wallet.loadFromFile(wf, null));
                } catch (UnreadableWalletException e) {
                    try {
                        wallets.put(wf, loadFromBackup(x));
                    } catch (UnreadableWalletException e1) {
                        Log.e(TAG, "Failed to load wallet " + wf.getAbsolutePath());
                    }
                }
            } else {
                try {
                    wallets.put(wf, loadFromBackup(x));
                } catch (UnreadableWalletException e1) {
                    Log.e(TAG, "Failed to load wallet " + wf.getAbsolutePath());
                }
            }
        }

        return new WalletHelper(wallets);
    }

    private static Wallet loadFromBackup(int num) throws UnreadableWalletException {
        if (num == 1) {
            return Wallet.loadFromFile(Constants.WALLET.MAIN_WALLET_BACKUP_FILE);
        } else {
            File wb = new File(Constants.WALLET.WALLET_BACKUP_FILES_NAME + num + Constants.FILES.FILENAME_NETWORK_SUFFIX);
            return Wallet.loadFromFile(wb);
        }
    }

    public static WalletHelper fromBackupWallets() throws UnreadableWalletException {
        HashMap<File, Wallet> wallets = new HashMap<>();
        try {
            wallets.put(Configuration.getInstance().getMainWalletFile(), loadFromBackup(1));
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }

        for (int x = 2; x <= Constants.WALLET.MAX_HD_ACCOUNTS; x++) {
            File wb = new File(Constants.WALLET.WALLET_BACKUP_FILES_NAME + x + Constants.FILES.FILENAME_NETWORK_SUFFIX);
            File wf = new File(Constants.WALLET.WALLET_FILES_NAME + x + Constants.FILES.FILENAME_NETWORK_SUFFIX);
            if (wb.exists()) {
                try {
                    wallets.put(wf, Wallet.loadFromFile(wb, null));
                } catch (UnreadableWalletException e) {
                    e.printStackTrace();
                }
            }
        }

        if (wallets.size() <= 0) {
            throw new UnreadableWalletException("Cannot read wallets!");
        }

        return new WalletHelper(wallets);
    }

    public List<Transaction> getPendingTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        for (Wallet w : wallets.values()) {
            transactions.addAll(w.getPendingTransactions());
        }

        return transactions;
    }
}
