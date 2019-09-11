package crea.wallet.lite.ui.tool;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import crea.wallet.lite.R;
import crea.wallet.lite.swap.api.ApiError;
import crea.wallet.lite.swap.process.AccountManager;
import crea.wallet.lite.util.task.Task;
import crea.wallet.lite.util.wrapper.DialogFactory;

public class SwapLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_login);

        Button loginButton = findViewById(R.id.login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLogin();
            }
        });
    }

    private void startLogin() {
        EditText eUsername = findViewById(R.id.swap_username);
        final String username = eUsername.getText().toString();

        EditText ePassword = findViewById(R.id.swap_password);
        String password = ePassword.getText().toString();

        final ProgressDialog pDialog = DialogFactory.progress(this, R.string.swap_login, R.string.swap_checking_account);
        pDialog.show();

        AccountManager accountManager = new AccountManager(username);
        try {
            accountManager.login(password, new Task<ApiError>() {
                @Override
                public void doTask(ApiError apiError) {
                    pDialog.dismiss();
                    if (apiError != null) {
                        DialogFactory.error(SwapLoginActivity.this, apiError).show();
                    } else {
                        Intent balanceIntent = new Intent(SwapLoginActivity.this, SwapBalanceActivity.class);
                        balanceIntent.putExtra("username", username);
                        startActivity(balanceIntent);
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onDestroy() {
        DialogFactory.removeDialogsFrom(getClass());
        super.onDestroy();
    }
}
