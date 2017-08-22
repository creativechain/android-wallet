package crea.wallet.lite.util;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import crea.wallet.lite.ui.tool.PinActivity;
import crea.wallet.lite.ui.tool.QRScannerActivity;
import crea.wallet.lite.ui.tool.RBFActivity;
import crea.wallet.lite.ui.tool.SeedActivity;
import com.gotcreations.materialpin.managers.AppLock;

import org.creativecoinj.core.Transaction;

/**
 * Created by ander on 10/11/16.
 */
public class IntentUtils {

    private static final String TAG = "IntentUtils";
    public static final int QR_SCAN = 1;
    public static final int IMPORT_SEED = 2;
    public static final int RETYPE_SEED = 3;
    public static final int PIN = 4;

    public static void startQRScanner(Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), QRScannerActivity.class);
        fragment.startActivityForResult(intent, QR_SCAN);
    }

    public static void startQRScanner(Activity activity) {
        Intent intent = new Intent(activity, QRScannerActivity.class);
        activity.startActivityForResult(intent, QR_SCAN);
    }

    public static void inputSeed(Activity activity) {
        inputSeed(activity, false);
    }

    public static void inputSeed(Activity activity, boolean retype) {
        Intent seedIntent = new Intent(activity, SeedActivity.class);
        seedIntent.putExtra(SeedActivity.RETYPE_SEED, retype);
        activity.startActivityForResult(seedIntent, retype ? RETYPE_SEED : IMPORT_SEED);
    }

    public static void createPin(Activity activity) {
        Intent pinIntent = new Intent(activity, PinActivity.class);
        pinIntent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK);
        activity.startActivityForResult(pinIntent, PIN);
    }

    public static void changePin(Activity activity) {
        Intent pinIntent = new Intent(activity, PinActivity.class);
        pinIntent.putExtra(AppLock.EXTRA_TYPE, AppLock.CHANGE_PIN);
        activity.startActivityForResult(pinIntent, PIN);
    }

    public static void checkPin(Activity activity) {
        Intent pinIntent = new Intent(activity, PinActivity.class);
        pinIntent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
        activity.startActivityForResult(pinIntent, PIN);
    }

    public static void raiseFee(Activity activity, Transaction tx) {
        Intent feeIntent = new Intent(activity, RBFActivity.class);
        feeIntent.putExtra(RBFActivity.TRANSACTION_ID, tx.getHash());
        activity.startActivity(feeIntent);
    }
}
