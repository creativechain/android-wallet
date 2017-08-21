package crea.wallet.lite.util;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import crea.wallet.lite.ui.tool.PinActivity;
import crea.wallet.lite.ui.tool.QRScannerActivity;
import crea.wallet.lite.ui.tool.SeedActivity;
import com.gotcreations.materialpin.managers.AppLock;

/**
 * Created by ander on 10/11/16.
 */
public class IntentUtils {

    private static final String TAG = "IntentUtils";
    public static final int QR_SCAN = 1;
    public static final int IMPORT_SEED = 2;
    public static final int PIN = 3;

    public static void startQRScanner(Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), QRScannerActivity.class);
        fragment.startActivityForResult(intent, QR_SCAN);
    }

    public static void startQRScanner(Activity activity) {
        Intent intent = new Intent(activity, QRScannerActivity.class);
        activity.startActivityForResult(intent, QR_SCAN);
    }

    public static void inputSeed(Activity activity) {
        Intent seedIntent = new Intent(activity, SeedActivity.class);
        activity.startActivityForResult(seedIntent, IMPORT_SEED);
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
}
