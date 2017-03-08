package crea.wallet.lite.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;


import crea.wallet.lite.R;

import java.util.Arrays;

/**
 * Created by ander on 17/02/16.
 */
public class Permissions {

    private static final String TAG = "Permissions";

    private static int checkPermissions(@NonNull Context context, @NonNull String... permissions) {
        int granted = 0;
        for (String s : permissions) {
            granted = ContextCompat.checkSelfPermission(context, s);
        }

        return granted;
    }

    public static boolean checkPermission(@NonNull final Activity context, @NonNull String permission, @NonNull String explanation, final int requestCode, @NonNull final String... permissions) {
        int granted = checkPermissions(context, permissions);
        Log.e(TAG, "Check self " + Arrays.toString(permissions) + ": " + (granted == PackageManager.PERMISSION_GRANTED));
        if (granted != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "PERMISSION_DENIED");
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permissions[0])) {
                AlertDialog a = DialogFactory.alert(context, permission, explanation);
                a.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(context, permissions, requestCode);
                    }
                });
                a.show();
            } else {
                ActivityCompat.requestPermissions(context, permissions, requestCode);
            }
            return false;
        } else {
            return true;
        }

    }

    public static boolean checkCameraPermission(Activity activity) {
        return checkPermission(activity,
                activity.getString(R.string.camera_permission),
                activity.getString(R.string.explanations_camera), 1,  Manifest.permission.CAMERA);
    }

    public static boolean checkSMSPermission(Activity activity) {
        return checkPermission(activity,
                activity.getString(R.string.enable_permissions),
                activity.getString(R.string.explanations_sms), 1,  Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS);
    }

    public static boolean checkLocationPermission(Activity activity) {
        return checkPermission(activity,
                activity.getString(R.string.enable_permissions),
                activity.getString(R.string.explanations_location), 2,  Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean checkContactsPermission(Activity activity) {
        return checkPermission(activity,
                activity.getString(R.string.enable_permissions),
                activity.getString(R.string.explanations_contacts), 1,  Manifest.permission.READ_CONTACTS);
    }

    public static boolean checkPhoneStatePermission(Activity activity) {
        return checkPermission(activity,
                activity.getString(R.string.enable_permissions),
                activity.getString(R.string.explanations_phone_state), 1, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WAKE_LOCK);
    }

    public static boolean checkStoragePermission(Activity activity) {
        return checkPermission(activity,
                activity.getString(R.string.storage_permission),
                activity.getString(R.string.explanations_storage), 1, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }


}
