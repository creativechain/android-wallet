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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import crea.wallet.lite.R;

import net.glxn.qrgen.android.QRCode;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.Coin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

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

    public static Bitmap fromAddress(String address, Coin amount) {
        return fromUri(Uri.parse("creativecoin:" + address + (amount != null ? "?amount=" + amount.toPlainString() : "")));
    }

    public static Bitmap fromAddress(Address address) {
        return fromAddress(address.toString(), null);
    }

    public static AlertDialog getQrDialog(final Activity activity, Bitmap qr, int titleId, final String text) {
        boolean hasText = text == null || !text.isEmpty();

        View v = LayoutInflater.from(activity).inflate(R.layout.qrdialog, null);

        ImageView qrView = (ImageView) v.findViewById(R.id.qrview);
        qrView.setImageBitmap(qr);

        if (hasText) {
            TextView addressView = (TextView) v.findViewById(R.id.text);
            addressView.setText(text);
        }

        AlertDialog alertDialog = DialogFactory.alert(activity, titleId, v);

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

                    Toast.makeText(activity, R.string.copied, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return alertDialog;
    }

    public static AlertDialog getAddressQrDialog(Activity activity, String data) {
        if (!data.startsWith("creativecoin:")) {
            data = "creativecoin:" + data;
        }
        return getQrDialog(activity, fromString(data), R.string.bitcoin_address, null);
    }

    public static AlertDialog getAddressQrDialog(final Activity activity, final Address address) {
        return getQrDialog(activity, fromAddress(address), R.string.bitcoin_address, address.toString());
    }

    public static AlertDialog getPrivKeyQrDialog(final Activity activity, String privKey) {
        return getQrDialog(activity, fromString(privKey), R.string.private_key, privKey);
    }

    public static AlertDialog getMnemonicQrDialog(Activity activity, String mnemonic) {
        return getQrDialog(activity, fromString(mnemonic), R.string.mnemonic_code, null);
    }

    public static AlertDialog getTransactionQrDialog(Activity activity, TxInfo txInfo) {
        byte[] txBytes = txInfo.getRaw();
        return getQrDialog(activity, fromString(encodeCompressBinary(txBytes)), R.string.transaction, txInfo.getHashAsString());
    }
    public static String encodeCompressBinary(final byte[] bytes) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            final GZIPOutputStream gos = new GZIPOutputStream(bos);
            gos.write(bytes);
            gos.close();

            final byte[] gzippedBytes = bos.toByteArray();
            final boolean useCompression = gzippedBytes.length < bytes.length;

            return String.valueOf(useCompression ? 'Z' : '-') +
                    Base43.encode(useCompression ? gzippedBytes : bytes);
        } catch (final IOException x) {
            throw new RuntimeException(x);
        }
    }
}
