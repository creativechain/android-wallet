package crea.wallet.lite.ui.tool;

import android.os.Bundle;
import android.util.Log;

import com.github.orangegangsters.lollipin.lib.enums.KeyboardButtonEnum;
import com.github.orangegangsters.lollipin.lib.interfaces.KeyboardButtonClickedListener;
import com.github.orangegangsters.lollipin.lib.managers.AppLockActivity;

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
        setResult(RESULT_OK);
        finish();
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
