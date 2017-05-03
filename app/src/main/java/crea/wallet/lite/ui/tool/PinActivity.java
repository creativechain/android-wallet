package crea.wallet.lite.ui.tool;

import android.os.Bundle;
import android.util.Log;

import com.gotcreations.materialpin.enums.KeyboardButtonEnum;
import com.gotcreations.materialpin.interfaces.KeyboardButtonClickedListener;
import com.gotcreations.materialpin.managers.AppLockActivity;

/**
 * Created by ander on 10/11/16.
 */
public class PinActivity extends AppLockActivity {

    private static final String TAG = "PinActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void showForgotDialog() {
        Log.d(TAG, "showForgotDialog");
    }

    @Override
    public void onPinFailure(int attempts) {
        Log.d(TAG, "onPinFailure");
        if (attempts >= 3) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onPinSuccess(int attempts) {
        Log.d(TAG, "onPinSuccess");
    }

    @Override
    public int getPinLength() {
        return 4;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }
}
