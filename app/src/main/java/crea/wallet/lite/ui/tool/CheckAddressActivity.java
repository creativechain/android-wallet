package crea.wallet.lite.ui.tool;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.AddressFormatException;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.wallet.WalletHelper;

public class CheckAddressActivity extends AppCompatActivity {

    private EditText address;
    private TextView checkResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_address);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        address = (EditText) findViewById(R.id.address);
        checkResult = (TextView) findViewById(R.id.check_result);

        address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String address = s.toString();
                checkAddress(address);
            }
        });
    }

    private void checkAddress(String addressString) {
        if (TextUtils.isEmpty(addressString)) {
            setResult(R.string.address_not_found, R.color.textColorPrimary);
        } else {
            Address address;
            try {
                address = Address.fromBase58(Constants.WALLET.NETWORK_PARAMETERS, addressString);
                boolean belongsWallet = WalletHelper.INSTANCE.getWallet().isPubKeyHashMine(address.getHash160());
                if (belongsWallet) {
                    setResult(R.string.address_belongs_wallet, R.color.green);
                } else {
                    setResult(R.string.address_no_belongs_wallet, R.color.colorAccent);
                }
            } catch (AddressFormatException e) {
                setResult(R.string.invalid_bitcoin_address, R.color.red);
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void setResult(@StringRes int text, @ColorRes int color) {
        checkResult.setText(text);
        checkResult.setTextColor(getResources().getColor(color));
    }
}
