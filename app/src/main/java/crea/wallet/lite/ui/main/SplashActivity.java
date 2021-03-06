package crea.wallet.lite.ui.main;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.ui.tool.SwapLoginActivity;
import crea.wallet.lite.util.task.WalletExporter;
import crea.wallet.lite.ui.tool.PinActivity;
import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.util.wrapper.IntentUtils;
import crea.wallet.lite.util.wrapper.Permissions;
import crea.wallet.lite.util.task.Task;
import crea.wallet.lite.wallet.WalletHelper;

import static crea.wallet.lite.application.WalletApplication.INSTANCE;

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

        INSTANCE.requestAccessToken();
    }

    private void launch() {
        if (WalletHelper.INSTANCE != null) {
            toHomeActivity();
        } else {
            toWelcomeActivity();
        }
    }

    private void toHomeActivity() {
        if (Configuration.getInstance().isNewSecurityModeActive()) {
            //WalletApplication.INSTANCE.startBlockchainService(true);

            Intent i = new Intent(this, SwapLoginActivity.class);
            startActivity(i);
            finish();
        } else {
            reenterPin();
        }

    }

    private void reenterPin() {
        AlertDialog aDialog = DialogFactory.alert(this, R.string.security, R.string.change_pin_for_security);
        aDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                IntentUtils.checkPin(SplashActivity.this);
            }
        });

        aDialog.show();
    }
    private void toWelcomeActivity() {
        INSTANCE.startBlockchainService(true);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                String pin = data.getStringExtra(PinActivity.EXTRA_CODE);
                final ProgressDialog pDialog = DialogFactory.progress(this, R.string.security, R.string.encrypting_wallet);
                pDialog.show();
                new WalletExporter(pin, new Task<Bundle>() {
                    @Override
                    public void doTask(Bundle bundle) {
                        if (bundle == null) {
                            Toast.makeText(SplashActivity.this, R.string.invalid_pin, Toast.LENGTH_LONG).show();
                            reenterPin();
                        } else {
                            Configuration.getInstance().setSecurityModeActive(true);
                            toHomeActivity();
                        }
                    }
                }, WalletExporter.MIGRATION) {
                    @Override
                    public void onPostExecute(Bundle data) {
                        pDialog.dismiss();
                        super.onPostExecute(data);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

    }
}
