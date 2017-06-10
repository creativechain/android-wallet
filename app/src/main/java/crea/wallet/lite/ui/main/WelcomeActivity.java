package crea.wallet.lite.ui.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.db.WalletCrypt;
import crea.wallet.lite.ui.tool.SeedActivity;
import crea.wallet.lite.util.DialogFactory;
import crea.wallet.lite.util.IntentUtils;
import crea.wallet.lite.wallet.WalletHelper;

import org.creativecoinj.core.Utils;
import org.creativecoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.List;


public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "WelcomeActivity";
    private List<String> seed;
    private long creationTime;

    private View tasks;
    private View options;
    private View progress;
    private TextView progressText;
    private TextView resultStatus;
    private boolean isNew = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        View optionCreate = findViewById(R.id.option_create_wallet);
        View optionGenerate = findViewById(R.id.option_generate_wallet);
        tasks = findViewById(R.id.tasks);
        options = findViewById(R.id.options);
        progressText = (TextView) findViewById(R.id.progress_text);
        resultStatus = (TextView) findViewById(R.id.result_status);
        progress = findViewById(R.id.progress);

        optionCreate.setOnClickListener(this);
        optionGenerate.setOnClickListener(this);
    }

    private void importSeed() {
        IntentUtils.inputSeed(this);
    }

    private void setProgressText(CharSequence text) {
        progressText.setText(text);
    }

    private void setProgressText(@StringRes int text) {
        progressText.setText(text);
    }

    private void showProgress(boolean show) {
        resultStatus.setVisibility(show ? View.GONE : View.VISIBLE);
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        options.setVisibility(show ? View.GONE : View.VISIBLE);
        tasks.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setResultStatus(boolean ok, CharSequence text) {
        int color = ok ? getResources().getColor(R.color.green) : getResources().getColor(R.color.red);
        resultStatus.setText(ok ? "\ue83b" : "\ue5c9");
        resultStatus.setTextColor(color);
        progressText.setText(text);
        showProgress(true);
        progress.setVisibility(View.GONE);
        resultStatus.setVisibility(View.VISIBLE);

    }

    private void generateSeed() {
        isNew = true;
        setProgressText(R.string.generating_seed);
        showProgress(true);
        try {
            seed = WalletApplication.INSTANCE.getMnemonicList();
            creationTime = System.currentTimeMillis();
            generateWallet();
        } catch (MnemonicException.MnemonicLengthException | IOException e) {
            setResultStatus(false, e.getMessage());
        }
    }

    private void generateWallet() {
        setProgressText(R.string.generating_wallet);
        showProgress(true);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                WalletHelper.INSTANCE = WalletHelper.create(seed, creationTime, true);
                publishProgress();
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                setResultStatus(true, getString(R.string.wallet_generated_correctly));
                if (isNew) {
                    showMnemonicCode();
                } else {
                    createPin();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


    }

    private void showMnemonicCode() {
        List<String> mnemonicCode = WalletHelper.INSTANCE.getKeyChainSeed().getMnemonicCode();
        String parsedList= TextUtils.join(", ", mnemonicCode);
        AlertDialog aDialog = DialogFactory.alert(this, R.string.mnemonic_code, getString(R.string.mnemonic_code_list, parsedList));
        aDialog.setCancelable(false);
        aDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                createPin();
            }
        });
        aDialog.show();
    }

    private void createPin() {
        AlertDialog aDialog = DialogFactory.alert(this, R.string.create_pin, R.string.create_pin_message);
        aDialog.setCancelable(false);
        aDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                IntentUtils.createPin(WelcomeActivity.this);
            }
        });
        aDialog.show();
    }

    private void cypherWallet(final String pin) {
        showProgress(true);
        setProgressText(R.string.encrypting_wallet);
        new AsyncTask<Void, Boolean, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    WalletCrypt walletCrypt = WalletCrypt.random();
                    walletCrypt.save();

                    Log.d(TAG, walletCrypt.toString());
                    Log.d(TAG, "Pass: " + pin);
                    String pass = walletCrypt.generate(pin);
                    Log.d(TAG, "Ecrypting with: " + Utils.HEX.encode(pass.getBytes()));
                    WalletHelper.INSTANCE.encrypt(pass);
                    WalletHelper.INSTANCE.save();
                    WalletApplication.INSTANCE.migrateBackup();
                    publishProgress(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    publishProgress(false);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Boolean... values) {
                boolean success = values[0];
                if (success) {
                    setResultStatus(true, getString(R.string.encrypt_wallet_success));
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            toHomeActivity();
                        }
                    }, 1000);
                } else {

                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private void toHomeActivity() {
        WalletApplication.INSTANCE.startBlockchainService(true);

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.option_create_wallet:
                generateSeed();
                break;
            case R.id.option_generate_wallet:
                importSeed();
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.IMPORT_SEED) {
                seed = data.getStringArrayListExtra(SeedActivity.EXTRA_SEED);
                Log.d(TAG, "IMPORTED SEED: " + seed.toString());
                generateWallet();
            } else if (requestCode == IntentUtils.PIN) {
                String pin = Configuration.getInstance().getPin();
                cypherWallet(pin);
            }
        }

    }
}
