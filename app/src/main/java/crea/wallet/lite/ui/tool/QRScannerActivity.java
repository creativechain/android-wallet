package crea.wallet.lite.ui.tool;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import crea.wallet.lite.R;
import crea.wallet.lite.util.Permissions;
import crea.wallet.lite.widget.qrscanner.ScannerView;
import com.google.zxing.Result;

/**
 * Created by ander on 22/04/15.
 */
public class QRScannerActivity extends Activity implements ScannerView.ResultHandler {

    private static final String TAG = "QRScannerActivity";
    private ScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_qrscanner);
        /*requestCode = getIntent().getExtras().getInt("requestCode");*/
        mScannerView = (ScannerView) findViewById(R.id.scannerView);
        mScannerView.setResultHandler(this);

        if (Permissions.checkCameraPermission(this)) {
            setCamera();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
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
        Log.d(TAG, "Starting camera");
        Camera camera = Camera.open();
        camera.startPreview();
        mScannerView.startCamera(camera);
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

        setResult(RESULT_OK, returnIntent);
        finish();
    }
}
