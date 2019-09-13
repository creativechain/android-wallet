package crea.wallet.lite.ui.base;

import androidx.appcompat.app.AppCompatActivity;

import crea.wallet.lite.util.wrapper.DialogFactory;

public abstract class BaseSwapActivity extends AppCompatActivity {

    @Override
    protected void onDestroy() {
        DialogFactory.removeDialogsFrom(getClass());
        super.onDestroy();
    }
}
