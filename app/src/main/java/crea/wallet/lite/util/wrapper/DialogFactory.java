package crea.wallet.lite.util.wrapper;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;


import crea.wallet.lite.R;
import crea.wallet.lite.swap.api.ApiError;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;


/**
 * Created by personal on 1/14/15.
 */
public class DialogFactory {

    private static final String TAG = "DialogFactory";

    private static HashMap<Class<? extends Context>, ArrayList<Dialog>> pool = new HashMap<>();

    private static void addDialog(Class<? extends Context> clazz, Dialog d) {
        ArrayList<Dialog> dialogs = pool.get(clazz);
        if (dialogs == null) {
            dialogs = new ArrayList<>();
        }

        dialogs.add(d);
        pool.put(clazz, dialogs);
    }

    private static AlertDialog.Builder alertBuilder(Activity activity) {
        return new AlertDialog.Builder(activity);
    }

    public static ProgressDialog progress(Activity activity, CharSequence title, CharSequence message) {
        if (activity != null && !activity.isFinishing()) {
            ProgressDialog p = new ProgressDialog(activity);
            p.setTitle(title);
            p.setMessage(message);
            p.setIcon(R.mipmap.ic_launcher);

            addDialog(activity.getClass(), p);
            return p;
        }

        return null;
    }

    public static ProgressDialog progress(Activity activity, @StringRes int title, @StringRes int message) {
        return progress(activity, activity.getString(title), activity.getString(message));
    }

    public static AlertDialog alert(Activity activity, CharSequence title, CharSequence message) {
        if (activity != null && !activity.isFinishing()) {
            AlertDialog.Builder aBuilder = alertBuilder(activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog aDialog = aBuilder.create();
            addDialog(activity.getClass(), aDialog);
            return aDialog;
        }

        return null;
    }

    public static AlertDialog alert(Activity activity, @StringRes int title, CharSequence message) {
        return alert(activity, activity.getString(title), message);
    }

    public static AlertDialog alert(Activity activity, CharSequence title, @StringRes int message) {
        return alert(activity, title, activity.getString(message));
    }

    public static AlertDialog alert(Activity activity, @StringRes int title, @StringRes int message) {
        return alert(activity, activity.getString(title), activity.getString(message));
    }

    public static AlertDialog alert(Activity activity, CharSequence title, View customView) {
        if (activity != null && !activity.isFinishing()) {
            AlertDialog.Builder aBuilder = alertBuilder(activity)
                    .setTitle(title)
                    .setView(customView);
            AlertDialog ad = aBuilder.create();
            addDialog(activity.getClass(), ad);
            return ad;
        }

        return null;
    }

    public static AlertDialog alert(Activity activity, @StringRes int title, View customView) {
        return alert(activity, activity.getString(title), customView);
    }

    public static AlertDialog list(Activity activity, @StringRes int title, String... options) {
        return list(activity, activity.getString(title), options);
    }

    public static AlertDialog list(Activity activity, CharSequence title, String... options) {
        if (activity != null && !activity.isFinishing()) {
            AlertDialog.Builder aBuilder = alertBuilder(activity)
                    .setTitle(title)
                    .setItems(options, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

            AlertDialog ad = aBuilder.create();
            addDialog(activity.getClass(), ad);
            return ad;
        }

        return null;
    }


    public static AlertDialog successAlert(Activity activity, @StringRes int title, View customView) {
        AlertDialog ad =alert(activity, title, customView);
        ad.setIcon(activity.getResources().getDrawable(R.drawable.ic_circle_color_green));
        return ad;
    }

    public static AlertDialog successAlert(Activity activity, @StringRes int title, @StringRes int message) {
        AlertDialog ad =alert(activity, title, message);
        ad.setIcon(activity.getResources().getDrawable(R.drawable.ic_circle_color_green));
        return ad;
    }

    public static AlertDialog successAlert(Activity activity, @StringRes int title, CharSequence message) {
        AlertDialog ad =alert(activity, title, message);
        ad.setIcon(activity.getResources().getDrawable(R.drawable.ic_circle_color_green));
        return ad;
    }

    public static AlertDialog successAlert(Activity activity, CharSequence title, @StringRes int message) {
        AlertDialog ad = alert(activity, title, message);
        ad.setIcon(activity.getResources().getDrawable(R.drawable.ic_circle_color_green));
        return ad;
    }

    public static AlertDialog successAlert(Activity activity, CharSequence title, CharSequence message) {
        AlertDialog ad = alert(activity, title, message);
        ad.setIcon(activity.getResources().getDrawable(R.drawable.ic_circle_color_green));
        return ad;
    }

    public static AlertDialog warn(Activity activity, CharSequence title, CharSequence message){
        AlertDialog ad = alert(activity, title, message);
        ad.setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setIcon(R.mipmap.ic_launcher);
        return ad;
    }

    public static AlertDialog warn(Activity activity, @StringRes int title, @StringRes int message){
        return warn(activity, activity.getString(title), activity.getString(message));
    }

    public static AlertDialog warn(Activity activity, CharSequence title, @StringRes int message){
        return warn(activity, title, activity.getString(message));
    }

    public static AlertDialog warn(Activity activity, @StringRes int title, CharSequence message){
        return warn(activity, activity.getString(title), message);
    }

    public static AlertDialog error(Activity activity, CharSequence title, CharSequence message){
        AlertDialog dialog = alert(activity, title, message);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setIcon(R.drawable.ic_circle_color_red);

        return dialog;

    }

    public static AlertDialog error(Activity activity, ApiError apiError) {
        switch (apiError) {
            case LOGIN_FAILED:
                return error(activity, "Error", "Invalid username or password.");
            default:
                return error(activity, "Error", "Unknown error");
        }
    }

    public static void removeDialogsFrom(Class<? extends Context> clazz) {
        ArrayList<Dialog> dialogs = pool.get(clazz);
        if (dialogs != null) {
            try {
                for (Dialog d : dialogs) {
                    if (d.isShowing()) {
                        d.dismiss();
                    }

                    dialogs.remove(d);
                }
            } catch (ConcurrentModificationException e) {
                Log.e(TAG, "Concurrent modification in list: " + e.getMessage());
                removeDialogsFrom(clazz);
            }
        }
        pool.remove(clazz);
    }
}
