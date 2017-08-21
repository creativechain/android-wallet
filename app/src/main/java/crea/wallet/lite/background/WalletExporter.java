package crea.wallet.lite.background;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.db.WalletCrypt;
import crea.wallet.lite.util.Task;
import crea.wallet.lite.wallet.WalletHelper;

import org.creativecoinj.wallet.DeterministicSeed;

import java.util.List;

/**
 * Created by ander on 25/06/15.
 */
public class WalletExporter extends AsyncTask<Void, Void, Bundle> {

    private static final String TAG = "WalletExporter";
    /**
     * Method to export the MnemonicCode valueOf a wallet.
     */
    public static final int MNEMONIC_CODE = 1;
    /**
     * Method to export the DeterministicSeed valueOf a wallet.
     */
    public static final int MIGRATION = 0;
    private String key;
    private Task<Bundle> task;
    private int mode = 0;

    /**
     * Create a WalletExporter that export a MnemonicCode or DeterministicSeed of wallet.
     * @param key the key of wallet.
     * @param task the Task that will be executed in Exception error.
     * @param mode the exporter mode. Can be: <b/><pre/>
     *             {@link WalletExporter#MNEMONIC_CODE}<b/><pre/>
     *             {@link WalletExporter#MIGRATION}
     */
    public WalletExporter(String key, Task<Bundle> task, int mode) {
        this.key = key;
        this.task = task;
        this.mode = mode;
    }

    /**
     * Create a WalletExporter that export a DeterministicSeed of wallet.
     * @param key the key of wallet.
     * @param task the Task that will be executed in Exception error.
     */
    public WalletExporter(String key, Task<Bundle> task) {
        this.key = key;
        this.task = task;
    }

    @Override
    protected Bundle doInBackground(Void... voids) {
        long creationTime;
        WalletCrypt walletCrypt = WalletCrypt.getInstance();
        String k = null;
        Bundle bundle = new Bundle();

        switch (mode) {
            case MIGRATION:
                try {
                    if (WalletHelper.INSTANCE.isWalletEncrypted()) {
                        k = walletCrypt.generate(key);
                        WalletHelper.INSTANCE.decrypt(k);
                    } else {
                        k = key;
                    }

                    WalletHelper.INSTANCE.encrypt(k);
                    WalletApplication.INSTANCE.migrateBackup(false);
                    return bundle;
                } catch (Throwable e) {
                    Log.e(TAG, "Failed to decrypt wallet", e);
                }

            case MNEMONIC_CODE:
                try {
                    if (WalletHelper.INSTANCE.isWalletEncrypted()) {
                        Log.d(TAG, "pass: " + key);
                        k = key;
                        Log.d(TAG, "Decrypting with: " + org.creativecoinj.core.Utils.HEX.encode(k.getBytes()));
                        WalletHelper.INSTANCE.decrypt(k);
                    } else {
                        Log.d(TAG, "Wallet not encrypted");
                    }

                    String seed;
                    DeterministicSeed dSeed = WalletHelper.INSTANCE.getKeyChainSeed();
                    seed = getMnemonicCodeAsString(dSeed.getMnemonicCode());
                    creationTime = dSeed.getCreationTimeSeconds();

                    Log.i(TAG, "seed: " + seed + ", creation time: " + creationTime);


                    WalletHelper.INSTANCE.encrypt(k);
                    WalletHelper.INSTANCE.save();

                    bundle.putString("exported", seed);
                    bundle.putLong("creation_time", creationTime);
                    return bundle;

                } catch (Throwable e) {
                    Log.e(TAG, "Failed to decrypt wallet", e);
                }
        }

        return null;
    }

    private String getMnemonicCodeAsString(List<String> mnemonicCode) {
        Log.d(TAG, "seed: " + mnemonicCode);
        return TextUtils.join(" ", mnemonicCode);
    }

    @Override
    public void onPostExecute(Bundle data) {
        task.doTask(data);
    }
}
