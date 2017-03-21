package crea.wallet.lite.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import crea.wallet.lite.R;
import crea.wallet.lite.application.WalletApplication;
import com.chip_chap.services.cash.coin.BitCoin;

import net.glxn.qrgen.android.QRCode;

import org.creacoinj.core.Address;

import java.io.File;

/**
 * Created by ander on 29/01/16.
 */
public class QR {

    private static final String TAG = "QR";

    public static Bitmap fromString(String string) {
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 60, WalletApplication.INSTANCE.getResources().getDisplayMetrics()));
        File qrFile = QRCode.from(string).withSize(px, px).file();
        Bitmap b = BitmapFactory.decodeFile(qrFile.getAbsolutePath());
        qrFile.delete();
        return b;
    }

    public static Bitmap fromUri(Uri uri) {
        return fromString(uri.toString());
    }

    public static Bitmap fromBitcoinUri(String address, BitCoin amount) {
        return fromUri(Uri.parse("creacoin:" + address + (amount != null ? "?amount=" + amount.toPlainString() : "")));
    }

    public static Bitmap fromBitcoinUri(String address) {
        return fromBitcoinUri(address, null);
    }

    public static Bitmap fromBitcoinUri(Address address) {
        return fromBitcoinUri(address.toString(), null);
    }

    public static AlertDialog bitcoinQrDialog(final Activity activity, Bitmap qr, final String text) {
        boolean hasText = text == null || !text.isEmpty();
        LinearLayout dialogView = new LinearLayout(activity);
        dialogView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        dialogView.setOrientation(LinearLayout.VERTICAL);

        ImageView qrView = new ImageView(activity);
        qrView.setImageDrawable(new BitmapDrawable(qr));
        qrView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialogView.addView(qrView);

        if (hasText) {
            TextView addressView = new TextView(activity);
            addressView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addressView.setText(text);
            addressView.setGravity(Gravity.CENTER);
            addressView.setBackgroundColor(activity.getResources().getColor(android.R.color.transparent));
            addressView.setPadding(0, Utils.convertDpToPixel(5, activity), 0, Utils.convertDpToPixel(5, activity));
            dialogView.addView(addressView);
        }

        AlertDialog alertDialog = DialogFactory.alert(activity, R.string.bitcoin_address, dialogView);
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        if (hasText) {
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getString(android.R.string.copy), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(text);
                        Log.wtf(TAG, "Clipboard copy: " + clipboard.getText().toString());
                    } else {
                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(text, text);
                        clipboard.setPrimaryClip(clip);
                        Log.wtf(TAG, "Clipboard copy: " + clipboard.getPrimaryClip().toString());
                    }

                    Toast.makeText(activity, android.R.string.copy, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return alertDialog;
    }

    public static AlertDialog bitcoinQrDialog(Activity activity, String data) {
        if (!data.startsWith("creacoin:")) {
            data = "creacoin:" + data;
        }
        return bitcoinQrDialog(activity, fromString(data), null);
    }

    public static AlertDialog bitcoinQrDialog(final Activity activity, final Address address) {
        return bitcoinQrDialog(activity, fromBitcoinUri(address), address.toString());
    }

}
