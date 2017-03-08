package crea.wallet.lite.util;

import android.app.Activity;
import android.content.Intent;

import crea.wallet.lite.ui.tool.PinActivity;
import crea.wallet.lite.ui.tool.QRScannerActivity;
import crea.wallet.lite.ui.tool.SeedActivity;
import com.github.orangegangsters.lollipin.lib.managers.AppLock;

/**
 * Created by ander on 10/11/16.
 */
public class IntentUtils {

    private static final String TAG = "IntentUtils";
    public static final int QR_SCAN = 1;
    public static final int IMPORT_SEED = 2;
    public static final int PIN = 3;

    public static void startQRScanner(Activity activity) {
        Intent intent = new Intent(activity, QRScannerActivity.class);
        activity.startActivityForResult(intent, QR_SCAN);
    }

    public static void inputSeed(Activity activity) {
        Intent seedIntent = new Intent(activity, SeedActivity.class);
        activity.startActivityForResult(seedIntent, IMPORT_SEED);
    }

    public static void createPin(Activity activity) {
        Intent pinIntetn = new Intent(activity, PinActivity.class);
        pinIntetn.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK);
        activity.startActivityForResult(pinIntetn, PIN);
    }

    public static void checkPin(Activity activity) {
        Intent pinIntetn = new Intent(activity, PinActivity.class);
        pinIntetn.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
        activity.startActivityForResult(pinIntetn, PIN);
    }
}
