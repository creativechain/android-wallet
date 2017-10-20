package crea.wallet.lite.application;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.app.Application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.crashlytics.android.Crashlytics;
import crea.wallet.lite.R;
import crea.wallet.lite.util.task.DynamicFeeLoader;
import crea.wallet.lite.util.task.PriceUpdater;
import crea.wallet.lite.service.CreativeCoinService;
import crea.wallet.lite.service.BlockchainService;
import crea.wallet.lite.ui.tool.PinActivity;
import crea.wallet.lite.util.Utils;
import crea.wallet.lite.wallet.WalletHelper;
import com.gotcreations.materialpin.managers.LockManager;

import io.fabric.sdk.android.Fabric;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.VerificationException;
import org.creativecoinj.crypto.MnemonicCode;
import org.creativecoinj.crypto.MnemonicException;
import org.creativecoinj.wallet.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

/**
 * Created by ander on 10/11/16.
 */
public class WalletApplication extends Application {

    private static final String TAG = "WalletApplication";
    private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

    public static WalletApplication INSTANCE;
    private ActivityManager activityManager;

    private Intent blockchainServiceIntent;
    private Intent blockchainServiceCancelCoinsReceivedIntent;
    private Intent blockchainServiceResetBlockchainIntent;
    private File walletFile;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        INSTANCE = this;
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        LockManager<PinActivity> lockManager = LockManager.getInstance();
        lockManager.enableAppLock(this, PinActivity.class);
        lockManager.getAppLock().setShouldShowForgot(false);
        lockManager.getAppLock().setLogoId(R.mipmap.ic_lock);

        initLogging();

        org.creativecoinj.core.Context.enableStrictMode();
        org.creativecoinj.core.Context.propagate(Constants.WALLET.CONTEXT);
        Log.d(TAG, "App in debug mode: " + Constants.TEST);

        initMnemonicCode();
        blockchainServiceIntent = new Intent(this, CreativeCoinService.class);
        blockchainServiceCancelCoinsReceivedIntent = new Intent(
                BlockchainService.ACTION_CANCEL_COINS_RECEIVED, null, this, CreativeCoinService.class);
        blockchainServiceResetBlockchainIntent = new Intent(
                BlockchainService.ACTION_RESET_BLOCKCHAIN, null, this, CreativeCoinService.class);

        new PriceUpdater().start();
        new DynamicFeeLoader(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        walletFile = Constants.WALLET.FIRST_WALLET_FILE;
        if (loadWalletHelper()) {
            afterLoadWallet();
        }

    }

    private void initLogging() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Logger log = context.getLogger(Logger.ROOT_LOGGER_NAME);

        final PatternLayoutEncoder logcatTagPattern = new PatternLayoutEncoder();
        logcatTagPattern.setContext(context);
        logcatTagPattern.setPattern("%logger{0}");
        logcatTagPattern.start();

        final PatternLayoutEncoder logcatPattern = new PatternLayoutEncoder();
        logcatPattern.setContext(context);
        logcatPattern.setPattern("[%thread] %msg%n");
        logcatPattern.start();

        final LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setTagEncoder(logcatTagPattern);
        logcatAppender.setEncoder(logcatPattern);
        logcatAppender.start();

        log.addAppender(logcatAppender);
        log.setLevel(Level.INFO);
    }

    public boolean isDebuggable() {
        boolean debuggable = true;

        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            Signature signatures[] = pinfo.signatures;

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (Signature signature : signatures) {
                ByteArrayInputStream stream = new ByteArrayInputStream(signature.toByteArray());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
                debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
                if (debuggable) {
                    break;
                }
            }
        } catch (PackageManager.NameNotFoundException | CertificateException e) {
            e.printStackTrace();
        }
        return debuggable;
    }

    private void initMnemonicCode()	{
        Log.e(TAG, "Initializing MnemonicCode");
        long t = System.currentTimeMillis();
        try	{
            String lang = Locale.getDefault().getCountry().toLowerCase();
            switch (lang) {
                case "es":
                case "fr":
                case "it":
                    break;
                default:
                    lang = "en";
                    break;
            }

            String path = "bitcoin/wordlist/" + lang + ".txt";
            MnemonicCode.INSTANCE = new MnemonicCode(getAssets().open(path), null);
        } catch (final IOException x) {
            throw new Error(x);
        }
        t = System.currentTimeMillis() - t;
        Log.e(TAG, "MnemonicCode initialized in " + t + "ms");
    }

    private boolean loadWalletHelper() {
        if (walletFile.exists()) {

            try	{
                Log.e(TAG, "Loading wallet...");
                WalletHelper.INSTANCE = WalletHelper.fromWallet();
                Log.e(TAG, "Wallets loaded successfully");
                if (!WalletHelper.INSTANCE.getWalletParams().equals(Constants.WALLET.NETWORK_PARAMETERS)) {
                    throw new UnreadableWalletException("bad wallet network parameters: " + WalletHelper.INSTANCE.getWalletParams().getId());
                }

            } catch (final UnreadableWalletException x)	{
                Log.e(TAG, "Problem loading wallet", x);
                Toast.makeText(WalletApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();

                restoreWalletFromBackup();
            }

            if (!WalletHelper.INSTANCE.isConsistentWallet()) {
                Log.e(TAG, "Inconsistent wallet: " + walletFile.getAbsolutePath());
                Toast.makeText(this, "inconsistent wallet: " + walletFile, Toast.LENGTH_LONG).show();
                return  restoreWalletFromBackup();
            } else {
                Log.e(TAG, "Wallet is consistent");
            }

            if (!WalletHelper.INSTANCE.getWalletParams().equals(Constants.WALLET.NETWORK_PARAMETERS)) {
                throw new Error("bad wallet network parameters: " + WalletHelper.INSTANCE.getWalletParams().getId());
            }
            return true;
        } else if (Constants.WALLET.WALLET_BACKUP_FILE.exists()) {
            return restoreWalletFromBackup();
        }
        return false;
    }

    public void localBackupWallet()	{
        if (WalletHelper.INSTANCE != null) {
            WalletHelper.INSTANCE.createBackup();
        }
    }

    private boolean restoreWalletFromBackup() {
        Log.e(TAG, "Restoring wallet from backup");

        try	{
            WalletHelper.INSTANCE = WalletHelper.loadFromBackup();
            WalletHelper.INSTANCE.cleanup();
            resetBlockchain();
            Log.i(TAG, "wallet restored from backup: '" + Constants.WALLET.WALLET_BACKUP_FILE + "'");
            return true;
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void afterLoadWallet() {
        WalletHelper.INSTANCE.cleanup();
        //WalletHelper.INSTANCE.autoSave(10);
        migrateBackup();
    }

    public void migrateBackup() {
        migrateBackup(true);
    }

    public void migrateBackup(boolean checkIfExist) {
        if (checkIfExist && Constants.WALLET.WALLET_BACKUP_FILE.exists()) {
            Log.e(TAG, "Wallet backup exist");
        } else {
            Log.d(TAG, "migrating automatic backup to protobuf");
            localBackupWallet();
        }
    }


    public List<String> getMnemonicList() throws MnemonicException.MnemonicLengthException, IOException {
        final byte[] MNEMONIC_BYTES = Utils.getRandomByteArray(Utils.MNEMONIC_BYTE_ARRAY_LENGHT);
        Log.e(TAG, "Bytes: " + (MNEMONIC_BYTES == null));
        return MnemonicCode.INSTANCE.toMnemonic(MNEMONIC_BYTES);
    }

    public void processDirectTransaction(final Transaction tx) throws VerificationException {
        if (WalletHelper.INSTANCE.isTransactionRelevant(tx)) {
            WalletHelper.INSTANCE.receivePending(tx, null);
            broadcastTransaction(tx);
        }
    }

    public void broadcastTransaction(final Transaction tx)	{
        final Intent intent = new Intent(BlockchainService.ACTION_BROADCAST_TRANSACTION, null, this, CreativeCoinService.class);
        intent.putExtra(BlockchainService.ACTION_BROADCAST_TRANSACTION_HASH, tx.getHash());
        startService(intent);
    }

    public boolean isLowRamDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            return activityManager.isLowRamDevice();
        else
            return activityManager.getMemoryClass() <= Constants.MEMORY_CLASS_LOWEND;
    }

    public int getScryptIterations() {
        return isLowRamDevice() ? 32768 : 65536;
    }

    public int maxConnectedPeers() 	{

        final int memoryClass = activityManager.getMemoryClass();
        if (memoryClass <= Constants.MEMORY_CLASS_LOWEND) {
            return 4;
        } else {
            return 6;
        }
    }

    public static void scheduleStartBlockchainService(final Context context) {
        final Configuration config = Configuration.getInstance();
        final long lastUsedAgo = config.getLastUsedAgo();

        // apply some backoff
        final long alarmInterval = 60 * 15 * 1000;

        Log.i(TAG, "last used " + (lastUsedAgo / DateUtils.MINUTE_IN_MILLIS) + " minutes ago, rescheduling blockchain sync in roughly " + (alarmInterval / DateUtils.MINUTE_IN_MILLIS) + " minutes");

        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent alarmIntent = PendingIntent.getService(context, 0, new Intent(context, CreativeCoinService.class), 0);
        alarmManager.cancel(alarmIntent);

        // workaround for no inexact set() before KitKat
        final long now = System.currentTimeMillis();
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, now + alarmInterval, AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    public void startBlockchainService(final boolean cancelCoinsReceived) {
        if (walletFile.exists()) {
            if (cancelCoinsReceived) {
                startService(blockchainServiceCancelCoinsReceivedIntent);
            } else {
                startService(blockchainServiceIntent);
            }
        }
    }

    public void stopBlockchainService() {
        stopService(blockchainServiceIntent);
    }

    public void resetBlockchain() {
        // implicitly stops blockchain service
        startService(blockchainServiceResetBlockchainIntent);
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        Log.i("TAG", "Multidex files installed!");
    }
}
