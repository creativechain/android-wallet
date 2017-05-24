package crea.wallet.lite.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import crea.wallet.lite.R;

import com.chip_chap.services.cash.coin.BitCoin;

import net.glxn.qrgen.android.QRCode;

import org.creativecoinj.core.Address;

import java.io.ByteArrayOutputStream;

import static crea.wallet.lite.application.WalletApplication.INSTANCE;

/**
 * Created by ander on 29/01/16.
 */
public class QR {

    private static final String TAG = "QR";

    public static Bitmap fromString(String string) {
        int px = (int) INSTANCE.getResources().getDimension(R.dimen.qr_code_size);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = px;
        options.outWidth = px;
        ByteArrayOutputStream baos = QRCode.from(string).withSize(px, px).stream();
        return BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size(), options);
    }

    public static Bitmap fromUri(Uri uri) {
        return fromString(uri.toString());
    }

    public static Bitmap fromCoinUri(String address, BitCoin amount) {
        return fromUri(Uri.parse("creativecoin:" + address + (amount != null ? "?amount=" + amount.toPlainString() : "")));
    }

    public static Bitmap fromCoinUri(String address) {
        return fromCoinUri(address, null);
    }

    public static Bitmap fromCoinUri(Address address) {
        return fromCoinUri(address.toString(), null);
    }

    public static AlertDialog getCoinQrDialog(final Activity activity, Bitmap qr, final String text) {
        boolean hasText = text == null || !text.isEmpty();
        LinearLayout dialogView = new LinearLayout(activity);
        dialogView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        dialogView.setOrientation(LinearLayout.VERTICAL);

        ImageView qrView = new ImageView(activity);
        qrView.setImageBitmap(qr);
        qrView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialogView.addView(qrView);

        if (hasText) {
            TextView addressView = new TextView(activity);
            addressView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addressView.setText(text);
            addressView.setGravity(Gravity.CENTER);
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
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(text, text);
                    clipboard.setPrimaryClip(clip);
                    Log.wtf(TAG, "Clipboard copy: " + clipboard.getPrimaryClip().toString());

                    Toast.makeText(activity, android.R.string.copy, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return alertDialog;
    }

    public static AlertDialog getCoinQrDialog(Activity activity, String data) {
        if (!data.startsWith("creativecoin:")) {
            data = "creativecoin:" + data;
        }
        return getCoinQrDialog(activity, fromString(data), null);
    }

    public static AlertDialog getCoinQrDialog(final Activity activity, final Address address) {
        return getCoinQrDialog(activity, fromCoinUri(address), address.toString());
    }

}
