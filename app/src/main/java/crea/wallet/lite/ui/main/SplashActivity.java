package crea.wallet.lite.ui.main;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import crea.wallet.lite.R;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.util.Permissions;
import crea.wallet.lite.wallet.WalletHelper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (Permissions.checkStoragePermission(this)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    launch();
                }
            }, 1500);
        }
    }

    private void launch() {
        if (WalletHelper.INSTANCE != null) {
            toHomeActivity();
        } else {
            toWelcomeActivity();
        }
    }

    private void toHomeActivity() {
        WalletApplication.INSTANCE.startBlockchainService(true);

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    private void toWelcomeActivity() {
        WalletApplication.INSTANCE.startBlockchainService(true);

        Intent i = new Intent(this, WelcomeActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launch();
        }
    }
}
