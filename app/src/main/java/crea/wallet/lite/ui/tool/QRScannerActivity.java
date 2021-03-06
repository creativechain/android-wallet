package crea.wallet.lite.ui.tool;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;

import crea.wallet.lite.R;
import crea.wallet.lite.util.wrapper.Permissions;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import com.google.zxing.Result;

/**
 * Created by ander on 22/04/15.
 */
public class QRScannerActivity extends Activity implements ZXingScannerView.ResultHandler {

    private static final String TAG = "QRScannerActivity";
    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_qrscanner);

        if (Permissions.checkCameraPermission(this)) {
            setCamera();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null) {
            mScannerView.stopCamera();           // Stop camera on pause
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setCamera();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void setCamera() {
        mScannerView = (ZXingScannerView) findViewById(R.id.scanner_view);
        mScannerView.setResultHandler(this);

        Log.d(TAG, "Starting camera");
/*        Camera camera = Camera.open();
        camera.startPreview();*/
        mScannerView.startCamera();
    }

    @Override
    public void handleResult(Result result) {
        String stringResult = result.getText();
        Intent returnIntent = new Intent();
        try {
            final Uri uri = Uri.parse(stringResult);
            returnIntent.setData(uri);
            Log.i(TAG, "URI detected: " + uri.toString());
        } catch (Exception e) {
            returnIntent.putExtra(Intent.EXTRA_TEXT, stringResult);
            Log.i(TAG, "String detected: " + stringResult);
        }

        mScannerView.stopCamera();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}
