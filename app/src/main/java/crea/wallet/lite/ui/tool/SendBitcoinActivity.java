package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.util.CoinConverter;
import crea.wallet.lite.util.DialogFactory;
import crea.wallet.lite.util.FormUtils;
import crea.wallet.lite.util.IntentUtils;
import crea.wallet.lite.util.OnTextChangeListener;
import crea.wallet.lite.wallet.AbstractPaymentProcessListener;
import crea.wallet.lite.wallet.FeeCalculation;
import crea.wallet.lite.wallet.PaymentProcess;
import crea.wallet.lite.wallet.PaymentProcessListener;
import crea.wallet.lite.wallet.WalletHelper;
import crea.wallet.lite.wallet.WalletUtils;
import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.coin.BitCoin;
import com.chip_chap.services.task.Task;
import com.chip_chap.services.transaction.Btc2BtcTransaction;
import com.chip_chap.services.util.Tags;

import org.creacoinj.core.Address;
import org.creacoinj.core.Coin;
import org.creacoinj.core.Transaction;
import org.creacoinj.core.TransactionConfidence;
import org.creacoinj.uri.BitcoinURI;
import org.creacoinj.uri.BitcoinURIParseException;
import org.creacoinj.wallet.SendRequest;
import org.creacoinj.wallet.Wallet;

import static crea.wallet.lite.application.Constants.WALLET.DONATION_ADDRESS;
import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.TRANSACTION_RECEIVED;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.TRANSACTION_SENT;

public class SendBitcoinActivity extends AppCompatActivity {

    private static final String TAG = "InputW2WActivity";

    private final BlockchainBroadcastReceiver TRANSACTION_RECEIVER = new BlockchainBroadcastReceiver() {
        @Override
        public void onTransactionSend(Btc2BtcTransaction transaction) {
            wallet = WalletHelper.INSTANCE.getMainWallet();
        }

        @Override
        public void onTransactionReceived(Btc2BtcTransaction transaction) {
            wallet = WalletHelper.INSTANCE.getMainWallet();
        }
    };

    private final PaymentProcessListener CONFIDENCE_LISTENER = new AbstractPaymentProcessListener() {
        @Override
        public void onSigning() {
            setState(State.SIGNING);
        }

        @Override
        public void onSending() {
            setState(State.SENDING);
        }

        @Override
        public void onSuccess(Transaction tx) {
            SendBitcoinActivity.this.tx = tx;
            setState(State.SENT);
        }

        @Override
        public void onInsufficientMoney(Coin missing) {
            State result = State.FAILED;
            result.setExplRes(getString(R.string.insufficient_money_text, missing.toFriendlyString()));
            setState(result);
        }

        @Override
        public void onInvalidEncryptionKey() {
            State result = State.FAILED;
            result.setExplRes(getString(R.string.invalid_encryption_key));
            setState(result);
        }

        @Override
        public void onFailure(Exception exception) {
            State result = State.FAILED;
            result.setExplRes(exception.getLocalizedMessage());
            setState(result);
        }

        @Override
        public void onConfidenceChanged(TransactionConfidence confidence, ChangeReason reason) {
            TransactionConfidence.ConfidenceType type = confidence.getConfidenceType();
            int peers = confidence.numBroadcastPeers();
            State result = State.FAILED;
            switch (type) {
                case BUILDING:
                    if (peers > 1) {
                        result = State.SENT;
                    } else {
                        result = State.SENDING;
                        result.setExplRes(getString(R.string.broadcasting_transaction_in_nodes, peers));
                    }
                    break;
                case PENDING:
                    result = State.SENDING;
                    result.setExplRes(getString(R.string.broadcasting_transaction_in_nodes, peers));
                    break;
                case DEAD:
                    result = State.FAILED;
                    result.setExplRes(getString(R.string.already_spent_funds));
                    break;

            }

            setState(result);
        }
    };

    private Transaction tx;
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
    private Wallet wallet;
    private Currency currency;
    private Task<Void> keyTask;
    private Configuration conf;
    private Address address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_bitcoin);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(R.string.send_bitcoin);

        conf = new Configuration(this);

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

        String formattedFeeString = String.format(getResources().getString(R.string.transaction_fee_of), BitCoin.valueOf(0).toFriendlyString());
        feeTextView.setText(formattedFeeString);

        if (WalletHelper.isInstanceNull()) {
            WalletHelper.INSTANCE = WalletHelper.fromWallets();
        }

        wallet = WalletHelper.INSTANCE.getMainWallet();


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
        toFiatAmount.setHint(getString(R.string.amount_in_currency, conf.getMainCurrency().getCode()));
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

        Button acceptBtn = (Button) findViewById(R.id.wallet_accept_btn);
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
                amountEditText.setText(BitCoin.valueOf(amount.getValue()).toPlainString());
            }
        } catch (BitcoinURIParseException e) {
            try {
                String data = uri.toString();
                Address a = Address.fromBase58(Constants.WALLET.NETWORK_PARAMETERS, data);
                addressEditText.setText(a.toString());
            } catch (Exception e1) {
                Toast.makeText(this, R.string.not_found_valid_data, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    private void handleBundleExtras(Bundle extras) {
        currency = conf.getMainCurrency();
        if (extras != null) {
            if (extras.containsKey(Tags.CURRENCY)) {
                currency = (Currency) extras.getSerializable(Tags.CURRENCY);
            }

            String address = extras.getString(Tags.ADDRESS);
            if (address != null && !address.isEmpty()) {
                if (WalletUtils.isValidAddress(NETWORK_PARAMETERS, address)) {
                    addressEditText.setText(address);
                } else {
                    Toast.makeText(this, R.string.invalid_bitcoin_address, Toast.LENGTH_LONG).show();
                }
            }

            double amount = extras.getDouble(Tags.AMOUNT);
            if (amount > 0) {
                amountEditText.setText(String.valueOf(amount));
                amountEditText.requestFocus();
                calculateBitcoinFee();
            }
        }

    }

    private void enableEditTextAmount(boolean enable) {

        if (!enable) {
            amountEditText.requestFocus();
            FeeCalculation feeCalculation = new FeeCalculation(wallet);
            BitCoin amount = BitCoin.valueOf(feeCalculation.getTotalToSent().getValue());
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
            double amount = Double.parseDouble(number);
            if (amount > 0) {
                if (id == amountEditText.getId()) {
                    converter.amount(BitCoin.valueOf(amount)).price(conf.getPriceForMainCurrency());
                    toFiatAmount.setText(converter.toString());
                } else  {
                    double btcPrice = 1 / conf.getPriceForMainCurrency().getDoubleValue();
                    BitCoin price = BitCoin.valueOf(btcPrice);
                    converter.amount(com.chip_chap.services.cash.coin.base.Coin.fromCurrency(conf.getMainCurrency(), amount))
                            .price(price );
                    amountEditText.setText(converter.toString());
                }

                calculateBitcoinFee();
            } else {
                feeTextView.setTextColor(errorColor);
                amountEditText.setTextColor(errorColor);
                toFiatAmount.setTextColor(errorColor);
                feeTextView.setText(getString(R.string.amount_too_small));
            }

        } else {
            feeTextView.setTextColor(errorColor);
            feeTextView.setText(String.format(getResources().getString(R.string.enter_valid_amount), currency.getName()));
        }
    }

    private void calculateBitcoinFee() {
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
                feeCalculation = new FeeCalculation(wallet, getAddress());
            } else {
                feeCalculation = new FeeCalculation(wallet, getAddress(), amountToSent);
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

            setState(State.PREPARED);
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
            this.address = Address.fromBase58(NETWORK_PARAMETERS, addressEditText.getText().toString());
            processTransaction();
        }
    }

    public void processTransaction() {
        broadcastStatus.setVisibility(View.VISIBLE);
        keyTask = new Task<Void>() {
            @Override
            public void doTask(Void s) {

                PaymentProcess paymentProcess = new PaymentProcess(SendBitcoinActivity.this, sReq, conf.getMainWalletFile());
                paymentProcess.setProcessListener(CONFIDENCE_LISTENER)
                        .start();
            }
        };

        getPinFromUser();

    }

    private void setState(final State state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                    com.chip_chap.services.cash.coin.base.Coin feeConversion
                            = new CoinConverter()
                            .amount(BitCoin.valueOf(feeOutput.getValue()))
                            .price(conf.getBtcPrice(Currency.EUR)).getConversion();

                    destinyAddress.setText(getAddress().toString());
                    destinyAmount.setText(totalOutput.toFriendlyString());
                    feeAmountBtc.setText(feeOutput.toFriendlyString());
                    feeAmountFiat.setText(feeConversion.toFriendlyString());
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
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(TRANSACTION_RECEIVER);
        super.onPause();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(TRANSACTION_SENT);
        filter.addAction(TRANSACTION_RECEIVED);
        registerReceiver(TRANSACTION_RECEIVER, filter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                keyTask.doTask(null);
            } else if (requestCode == IntentUtils.QR_SCAN) {
                handleBitcoinUri(data.getData());
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == IntentUtils.PIN) {
                Toast.makeText(this, R.string.bad_pn_too_many_times, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        DialogFactory.removeDialogsFrom(getClass());
        super.onDestroy();
    }
}
