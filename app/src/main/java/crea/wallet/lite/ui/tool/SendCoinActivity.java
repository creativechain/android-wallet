package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.coin.CoinUtils;
import crea.wallet.lite.ui.main.PrepareTxActivity;
import crea.wallet.lite.util.CoinConverter;
import crea.wallet.lite.util.FormUtils;
import crea.wallet.lite.util.IntentUtils;
import crea.wallet.lite.util.OnTextChangeListener;
import crea.wallet.lite.util.Task;
import crea.wallet.lite.wallet.FeeCalculation;
import crea.wallet.lite.wallet.FeeCategory;
import crea.wallet.lite.wallet.PaymentProcess;
import crea.wallet.lite.wallet.WalletHelper;
import crea.wallet.lite.wallet.WalletUtils;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Address;
import org.creativecoinj.core.Coin;
import org.creativecoinj.uri.BitcoinURI;
import org.creativecoinj.uri.BitcoinURIParseException;
import org.creativecoinj.wallet.SendRequest;

import static crea.wallet.lite.application.Constants.WALLET.DONATION_ADDRESS;
import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;

public class SendCoinActivity extends PrepareTxActivity {

    private static final String TAG = "InputW2WActivity";

    private SendRequest sReq;
    private EditText addressEditText;
    private EditText amountEditText;
    private EditText toFiatAmount;
    private TextWatcher amountTextListener;
    private TextWatcher fiatTextListener;
    private TextView feeTextView;
    private TextView txStatus;
    private TextView txStatusIcon;
    private TextView destinyAddress;
    private TextView destinyAmount;
    private TextView feeAmountBtc;
    private TextView feeAmountFiat;
    private CheckBox sendAllMoney;
    private View broadcastStatus;
    private View acceptBtn;
    private String currency;
    private Task<String> keyTask;
    private Address address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_coin);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        feeTextView = (TextView) findViewById(R.id.transaction_fee);
        sendAllMoney = (CheckBox) findViewById(R.id.send_all_money);
        addressEditText = (EditText) findViewById(R.id.wallet_user_name_edit_text);
        amountEditText = (EditText) findViewById(R.id.amount);
        broadcastStatus = findViewById(R.id.broadcast_status);
        txStatus = (TextView) findViewById(R.id.transaction_status);
        txStatusIcon = (TextView) findViewById(R.id.transaction_status_icon);
        feeAmountBtc = (TextView) findViewById(R.id.fee_amount_btc);
        feeAmountFiat = (TextView) findViewById(R.id.fee_amount_fiat);
        destinyAddress = (TextView) findViewById(R.id.destination_address);
        destinyAmount = (TextView) findViewById(R.id.destination_amount);

        String formattedFeeString = String.format(getResources().getString(R.string.transaction_fee_of), Coin.valueOf(0).toFriendlyString());
        feeTextView.setText(formattedFeeString);

        if (WalletHelper.isInstanceNull()) {
            WalletHelper.INSTANCE = WalletHelper.fromWallet();
        }

        setUpFeeOptions();

        amountEditText.setHint(getString(R.string.amount_in_currency, "CREA"));
        amountEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    amountEditText.addTextChangedListener(amountTextListener);
                    toFiatAmount.removeTextChangedListener(fiatTextListener);
                } else {
                    amountEditText.removeTextChangedListener(amountTextListener);
                }
            }
        });

        toFiatAmount = (EditText) findViewById(R.id.to_fiat_amount);
        toFiatAmount.setHint(getString(R.string.amount_in_currency, conf.getMainCurrency()));
        toFiatAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    toFiatAmount.addTextChangedListener(fiatTextListener);
                    amountEditText.removeTextChangedListener(amountTextListener);
                } else {
                    toFiatAmount.removeTextChangedListener(fiatTextListener);
                }
            }
        });

        amountTextListener = new OnTextChangeListener() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String number = charSequence.toString();
                checkAvailableAmount(number, amountEditText);
            }
        };

        fiatTextListener = new OnTextChangeListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String number = s.toString();
                checkAvailableAmount(number, toFiatAmount);
            }
        };

        acceptBtn = findViewById(R.id.wallet_accept_btn);
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateFields();
            }
        });

        Uri uri = getIntent().getData();
        if (uri != null) {
            handleBitcoinUri(uri);
        } else {
            handleBundleExtras(getIntent().getExtras());
        }

        sendAllMoney.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableEditTextAmount(!isChecked);
            }
        });

    }

    private Address getAddress() {

        if (address != null) {
            return address;
        } else if (!FormUtils.isEmpty(addressEditText) && WalletUtils.isValidAddress(NETWORK_PARAMETERS, addressEditText.getText().toString())) {
            return Address.fromBase58(NETWORK_PARAMETERS, addressEditText.getText().toString());
        }

        return DONATION_ADDRESS;
    }

    private void handleBitcoinUri(Uri uri) {
        try {
            BitcoinURI btcUri = new BitcoinURI(uri.toString());
            currency = conf.getMainCurrency();
            Address address = btcUri.getAddress();
            Coin amount = btcUri.getAmount();
            if (address != null) {
                addressEditText.setText(btcUri.getAddress().toString());
            }

            if (amount != null) {
                amountEditText.requestFocus();
                amountEditText.setText(amount.toPlainString());
            }

            calculateFee();
        } catch (BitcoinURIParseException e) {
            try {
                String data = uri.toString();
                Address a = Address.fromBase58(Constants.WALLET.NETWORK_PARAMETERS, data);
                addressEditText.requestFocus();
                addressEditText.setText(a.toString());
                calculateFee();
            } catch (Exception e1) {
                Toast.makeText(this, R.string.not_found_valid_data, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    private void handleBundleExtras(Bundle extras) {
        currency = conf.getMainCurrency();
        if (extras != null) {
            if (extras.containsKey("currency")) {
                currency = extras.getString("currency");
            }

            String address = extras.getString("address");
            if (address != null && !address.isEmpty()) {
                if (WalletUtils.isValidAddress(NETWORK_PARAMETERS, address)) {
                    addressEditText.setText(address);
                } else {
                    Toast.makeText(this, R.string.invalid_bitcoin_address, Toast.LENGTH_LONG).show();
                }
            }

            double amount = extras.getDouble("amount");
            if (amount > 0) {
                amountEditText.setText(String.valueOf(amount));
                amountEditText.requestFocus();
                calculateFee();
            }
        }

    }

    private void setUpFeeOptions() {
        RadioButton economic = (RadioButton) findViewById(R.id.economic_fee);
        RadioButton normal = (RadioButton) findViewById(R.id.normal_fee);
        RadioButton priority = (RadioButton) findViewById(R.id.priority_fee);

        switch (this.feeCategory) {
            case ECONOMIC:
                economic.setChecked(true);
                break;
            case NORMAL:
                normal.setChecked(true);
                break;
            case PRIORITY:
                priority.setChecked(true);
                break;
        }

        economic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    feeCategory = FeeCategory.ECONOMIC;
                    calculateFee();
                }
            }
        });

        normal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    feeCategory = FeeCategory.NORMAL;
                    calculateFee();
                }
            }
        });

        priority.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    feeCategory = FeeCategory.PRIORITY;
                    calculateFee();
                }
            }
        });
    }

    private void enableEditTextAmount(boolean enable) {

        if (!enable) {
            amountEditText.requestFocus();
            FeeCalculation feeCalculation = new FeeCalculation(wallet, feeCategory);
            Coin amount = feeCalculation.getTotalToSent();
            amountEditText.setText(amount.toPlainString());
        }

        amountEditText.setEnabled(enable);
        toFiatAmount.setEnabled(enable);
    }

    private void checkAvailableAmount(String number, EditText editText) {
        /*number = number.substring(0, number.length() -1).trim();*/
        int id = editText.getId();
        int errorColor = getResources().getColor(R.color.red);

        if (number.matches("-?\\d+(\\.\\d+)?")) {

            CoinConverter converter = new CoinConverter();
            AbstractCoin price = conf.getPriceForMainCurrency();
            double amount = Double.parseDouble(number);
            if (amount > 0) {
                if (id == amountEditText.getId()) {
                    converter.amount(CoinUtils.valueOf("CREA", amount)).price(price);
                    toFiatAmount.setText(converter.toString());
                } else  {
                    double fiatDouble = 1 / price.getValue();
                    AbstractCoin fiatPrice = CoinUtils.valueOf("CREA", fiatDouble);
                    converter.amount(CoinUtils.valueOf(conf.getMainCurrency(), amount))
                            .price(fiatPrice);
                    amountEditText.setText(converter.toString());
                }

                calculateFee();
            } else {
                feeTextView.setTextColor(errorColor);
                amountEditText.setTextColor(errorColor);
                toFiatAmount.setTextColor(errorColor);
                feeTextView.setText(getString(R.string.amount_too_small));
            }

        } else {
            feeTextView.setTextColor(errorColor);
            feeTextView.setText(String.format(getResources().getString(R.string.enter_valid_amount), currency));
        }
    }

    private void calculateFee() {
        int normalColor = getResources().getColor(R.color.gray_2e2);
        int normalColorBlue = getResources().getColor(R.color.colorPrimary);
        int errorColor = getResources().getColor(R.color.red);

        feeTextView.setTextColor(normalColor);
        amountEditText.setTextColor(normalColorBlue);
        toFiatAmount.setTextColor(normalColorBlue);
        boolean emptyWallet = sendAllMoney.isChecked();

        if (FormUtils.containsDecimal(amountEditText)) {
            double btcAmount = Double.parseDouble(amountEditText.getText().toString());
            Coin amountToSent = Coin.valueOf(Math.round(btcAmount * 1e8d));
            FeeCalculation feeCalculation;

            if (emptyWallet) {
                feeCalculation = new FeeCalculation(wallet, getAddress(), this.feeCategory);
            } else {
                feeCalculation = new FeeCalculation(wallet, getAddress(), amountToSent, this.feeCategory);
            }

            Log.e(TAG, "calculation error " + feeCalculation.hasError() + ", has money: " + feeCalculation.hasSufficientMoney());
            if (!feeCalculation.hasError() && feeCalculation.hasSufficientMoney()) {
                String formattedFeeString = String.format(getResources().getString(R.string.transaction_fee_of), feeCalculation.getTxFee().toFriendlyString());
                feeTextView.setText(formattedFeeString);
                Log.i(TAG, "To donation address: " + feeCalculation.isToDonationAddress() + ", " + feeCalculation.getAddress().toBase58());
                if (!feeCalculation.isToDonationAddress()) {
                    sReq = feeCalculation.getsReq();
                }
            } else {
                feeTextView.setText(getResources().getString(R.string.no_sufficient_money, feeCalculation.getMissing().toFriendlyString()));
                feeTextView.setTextColor(errorColor);
                amountEditText.setTextColor(errorColor);
                toFiatAmount.setTextColor(errorColor);
            }


            onTransactionState(feeCalculation.isToDonationAddress() ? null : State.PREPARED);
        }

    }

    public void validateFields() {

        boolean hasError = false;
        String error = getResources().getString(R.string.empty_error_field);
        String address = addressEditText.getText().toString();

        if (FormUtils.isEmpty(addressEditText)) {
            addressEditText.setError(error);
            hasError = true;
        } else if (!WalletUtils.isValidAddress(NETWORK_PARAMETERS, address)) {
            addressEditText.setError(getString(R.string.invalid_bitcoin_address));
            hasError = true;
        }

        if (!FormUtils.containsDecimal(amountEditText)) {
            amountEditText.setError(error);
            hasError = true;
        }

        if (!hasError) {
            processTransaction();
        }
    }

    public void processTransaction() {
        broadcastStatus.setVisibility(View.VISIBLE);
        keyTask = new Task<String>() {
            @Override
            public void doTask(String s) {

                PaymentProcess paymentProcess = new PaymentProcess(SendCoinActivity.this, sReq, s);
                paymentProcess.setProcessListener(CONFIDENCE_LISTENER)
                        .start();
            }
        };

        getPinFromUser();

    }

    @Override
    public void onTransactionState(final State state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (state != null) {
                    broadcastStatus.setVisibility(View.VISIBLE);
                    CharSequence explRes = state.getExplRes() != null ? state.getExplRes() : "";

                    txStatus.setText(getString(state.toStringResource()) + "\n" + explRes);
                    txStatusIcon.setTextColor(getResources().getColor(state.toColorResource()));

                    if (state == State.SENT) {
                        View txDetails = findViewById(R.id.transaction_details);
                        View feeDetails = findViewById(R.id.fee_details);
                        txDetails.setVisibility(View.VISIBLE);
                        feeDetails.setVisibility(View.VISIBLE);

                        Coin totalOutput = tx.getOutputSum();
                        Coin feeOutput = tx.getFee();
                        AbstractCoin feeConversion
                                = new CoinConverter()
                                .amount(feeOutput)
                                .price(conf.getPriceForMainCurrency()).getConversion();

                        destinyAddress.setText(getAddress().toString());
                        destinyAmount.setText(totalOutput.toFriendlyString());
                        feeAmountBtc.setText(feeOutput.toFriendlyString());
                        feeAmountFiat.setText(feeConversion.toFriendlyString());
                        acceptBtn.setEnabled(false);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 3000);
                    }
                }

            }
        });
    }

    private void getPinFromUser() {
        IntentUtils.checkPin(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_btc_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_scan:
                launchAddressScannerIntent();
                break;
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchAddressScannerIntent() {
        IntentUtils.startQRScanner(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                keyTask.doTask(data.getStringExtra(PinActivity.EXTRA_CODE));
            } else if (requestCode == IntentUtils.QR_SCAN) {
                handleBitcoinUri(data.getData());
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == IntentUtils.PIN) {
                Toast.makeText(this, R.string.bad_pn_too_many_times, Toast.LENGTH_LONG).show();
            }
        }
    }
}
