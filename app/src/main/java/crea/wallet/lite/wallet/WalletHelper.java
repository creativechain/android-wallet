package crea.wallet.lite.wallet;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.common.collect.Lists;

import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.application.WalletApplication;


import org.creativecoinj.core.Address;
import org.creativecoinj.core.Block;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Context;
import org.creativecoinj.core.ECKey;
import org.creativecoinj.core.InsufficientMoneyException;
import org.creativecoinj.core.NetworkParameters;
import org.creativecoinj.core.Sha256Hash;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.crypto.ChildNumber;
import org.creativecoinj.crypto.DeterministicKey;
import org.creativecoinj.crypto.KeyCrypter;
import org.creativecoinj.crypto.MnemonicException;
import org.creativecoinj.wallet.CoinSelector;
import org.creativecoinj.wallet.DeterministicSeed;
import org.creativecoinj.wallet.KeyChain;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static crea.wallet.lite.application.Constants.WALLET.CONTEXT;
import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;

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
    private Wallet wallet;

    private WalletHelper(Wallet wallet) {
        this.wallet = wallet;
    }

    public boolean isWalletEncrypted() {
        return this.wallet.isEncrypted();
    }

    public void addEventListener(WalletEventListener listener, Executor executor) {
        if (executor == null) {
            executor = EXECUTOR;
        }
        wallet.addEventListener(listener, executor);

    }

    public void removeEventListener(WalletEventListener listener) {
        wallet.removeEventListener(listener);
    }

    public Address currentMainReceiveAddress() {
        return this.wallet.currentReceiveAddress();
    }

    public Address getNewAddress() {
        return this.wallet.freshReceiveAddress();
    }

    public Address getNewAddress(KeyChain.KeyPurpose keyPurpose) {
        return this.wallet.freshAddress(keyPurpose);
    }

    public List<ECKey> getReceivedKeys() {
        return this.wallet.getIssuedReceiveKeys();
    }

    public DeterministicKey getKey(Address address) {

        List<ECKey> keys = getReceivedKeys();
        for (ECKey ecKey : keys) {
            if (address.toString().equals(ecKey.toAddress(NETWORK_PARAMETERS).toString())) {
                return (DeterministicKey) ecKey;
            }
        }

        return null;
    }

    public int addIssuedAddressesToWatch() {
        return addWatchedAddresses(getReceiveAddresses());
    }

    public int addWatchedAddresses(List<Address> addresses) {
        return wallet.addWatchedAddresses(addresses, 0);
    }

    public List<Address> getReceiveAddresses() {
        List<Address> list = wallet.getIssuedReceiveAddresses();
        if (list.size() == 0) {
            list.add(wallet.freshReceiveAddress());
        }
        return list;
    }

    public List<Address> getAllAddressesForSwap() {
        List<Address> list = wallet.getIssuedReceiveAddresses();

        //Adding Receive Addresses
        while (list.size() < 100) {
            list.add(wallet.freshReceiveAddress());
        }

        //Getting change addresses
        int issuedKeys = wallet.getActiveKeyChain().getIssuedInternalKeys();
        for (int x = 0; x < issuedKeys; x++) {
            ChildNumber cn = new ChildNumber(x, false);
            List<ChildNumber> cns = new ArrayList<>();
            cns.add(ChildNumber.ZERO_HARDENED);
            cns.add(ChildNumber.ONE);
            cns.add(cn);
            ECKey key = wallet.getKeyByPath(cns);
            list.add(key.toAddress(NETWORK_PARAMETERS));
        }

        return list;
    }

    public Coin getBalance() {
        return this.wallet.getBalance();
    }

    public Coin getBalance(Wallet.BalanceType balanceType) {
        return this.wallet.getBalance(balanceType);
    }

    public Coin getBalance(CoinSelector coinSelector) {
        return this.wallet.getBalance(coinSelector);
    }

    public Coin getAddressBalance(Address address) {
        return getBalance(new AddressBalance(address));
    }

    public Coin getAddressBalance(String address) {
        return getAddressBalance(Address.fromBase58(Constants.WALLET.NETWORK_PARAMETERS, address));
    }

    public Map<String, String> signMessage(final String key, String message, Address... addresses) {
        try {
            Map<String, String> signedMessages = new HashMap<>();

            Context.propagate(CONTEXT);
            if (wallet.isEncrypted()) {
                wallet.decrypt(key);
            }

            Log.d(TAG, "Messages to sign: " + addresses.length);

            for (Address address : addresses) {
                ECKey ecKey = wallet.findKeyFromPubHash(address.getHash160());
                assert ecKey.toAddress(NETWORK_PARAMETERS).toBase58().equals(address.toBase58());
                String signedMessage = ecKey.signMessage(message);
                signedMessages.put(address.toBase58(), signedMessage);
            }

            Log.d(TAG, "Signed Messages: " + signedMessages.size());
            new Thread() {
                @Override
                public void run() {
                    Context.propagate(CONTEXT);
                    encrypt(key);
                }
            }.start();

            return signedMessages;

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void singTransaction(final CharSequence tryKey, SendRequest sReq) {
        try {
            Context.propagate(CONTEXT);
            if (wallet.isEncrypted()) {
                wallet.decrypt(tryKey);
            }
            wallet.signTransaction(sReq);
            new Thread() {
                @Override
                public void run() {
                    Context.propagate(CONTEXT);
                    encrypt(tryKey);
                }
            }.start();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public SendRequest prepareTransaction(final CharSequence tryKey, final CharSequence newKey, Coin coinsToSent, Address address, boolean emptyWallet) throws InsufficientMoneyException {

        try {
            wallet.decrypt(tryKey);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        SendRequest sReq = prepareTransaction(coinsToSent, address, emptyWallet);
        new Thread() {
            @Override
            public void run() {
                Context.propagate(CONTEXT);
                encrypt(newKey);
            }
        }.start();
        return sReq;
    }

    public SendRequest prepareTransaction(Coin coinsToSent, Address address, boolean emptyWallet) throws InsufficientMoneyException {
        Context.propagate(CONTEXT);
        SendRequest sReq;
        if (emptyWallet) {
            sReq = SendRequest.emptyWallet(address);
        } else {
            sReq = SendRequest.to(address, coinsToSent);
        }

        sReq.feePerKb = Configuration.getInstance().getTransactionFee();
        wallet.completeTx(sReq);
        return sReq;
    }

    public boolean isNewWallet() {
        return newWallet;
    }

    public void setNewWallet(boolean isNewWallet) {
        this.newWallet = isNewWallet;
    }

    public void commitTx(Transaction tx) {
        wallet.commitTx(tx);
    }

    public Transaction getTransaction(Sha256Hash hash) {
        return this.wallet.getTransaction(hash);
    }

    public boolean isTransactionRelevant(Transaction tx) {
        return this.wallet.isTransactionRelevant(tx);
    }

    public void receivePending(Transaction tx, List<Transaction> dependencies) {
        wallet.receivePending(tx, dependencies);
    }

    public void setLastBlock(Block block, int height) {
        wallet.setLastBlockSeenHash(block.getHash());
        wallet.setLastBlockSeenHeight(height);
        wallet.setLastBlockSeenTimeSecs(block.getTimeSeconds());
    }

    public int getLastBlockHeight() {
        return wallet.getLastBlockSeenHeight();
    }

    public long getKeyCreationTime() {
        return wallet.getEarliestKeyCreationTime();
    }

    public DeterministicSeed getKeyChainSeed(CharSequence key) {
        decrypt(key);
        DeterministicSeed seed = wallet.getKeyChainSeed();
        encrypt(key);
        return seed;
    }

    public DeterministicSeed getKeyChainSeed() {
        return wallet.getKeyChainSeed();
    }

    public Wallet getWallet() {
        return wallet;
    }

    public File getWalletFile() {
        return Constants.WALLET.FIRST_WALLET_FILE;
    }

    public KeyCrypter getKeyCrypter() {
        return wallet.getKeyCrypter();
    }

    public void encrypt(CharSequence key) {
        Context.propagate(CONTEXT);
        if (wallet.isEncrypted()) {
            Log.e(TAG, "Wallet is already encrypted.");
            return;
        }
        wallet.encrypt(key);
    }


    public void decrypt(CharSequence key) {
        Context.propagate(CONTEXT);
        if (wallet.isEncrypted()) {
            wallet.decrypt(key);
        }
    }

    public void createBackup()	{
        Log.e(TAG, "Creating local backup from wallet");
        long t = System.currentTimeMillis();
        final Protos.Wallet.Builder builder = new WalletProtobufSerializer().walletToProto(wallet).toBuilder();

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
            os = new FileOutputStream(Constants.WALLET.WALLET_BACKUP_FILE);
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

        try {
            File f = Constants.WALLET.FIRST_WALLET_FILE;
            wallet.saveToFile(f);
            Log.e(TAG, "Wallet saved in " + f.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Impossible save the wallet", e);
        }

    }

    public void autoSave(long delayTimeInSeconds) {
        try {
            wallet.autosaveToFile(Constants.WALLET.FIRST_WALLET_FILE, delayTimeInSeconds, TimeUnit.SECONDS, AUTOSAVE_LISTENER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        Log.e(TAG, "Cleaning wallet...");
        wallet.cleanup();
    }

    public void reset() {
        Log.e(TAG, "Resetting wallet...");
        wallet.reset();
    }

    public boolean isConsistentWallet() {
        return wallet.isConsistent();
    }

    public void shutdownAutosave() {
        wallet.shutdownAutosaveAndWait();
    }

    public NetworkParameters getWalletParams() {
        return WALLET_PARAMS;
    }

    public static boolean isInstanceNull() {
        return INSTANCE == null;
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

            Log.e(TAG, "WORDS=" + wordList.size());
            WalletGenerator walletGenerator = new WalletGenerator(wordList, creationTime).setIsNewAccount(true);
            walletGenerator.setForceOverride(override).create().saveInFiles();
            return setAndGetInstance(new WalletHelper(walletGenerator.getWallet()), true);
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

    public static WalletHelper fromWallet() {
        try {
            Wallet mainWallet = Wallet.loadFromFile(Constants.WALLET.FIRST_WALLET_FILE);
            return new WalletHelper(mainWallet);
        } catch (UnreadableWalletException e) {
            try {
                return loadFromBackup();
            } catch (UnreadableWalletException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    public static WalletHelper loadFromBackup() throws UnreadableWalletException {
        return new WalletHelper(Wallet.loadFromFile(Constants.WALLET.WALLET_BACKUP_FILE));
    }

    public List<Transaction> getPendingTransactions() {
        return Lists.newArrayList(this.wallet.getPendingTransactions());
    }
}
