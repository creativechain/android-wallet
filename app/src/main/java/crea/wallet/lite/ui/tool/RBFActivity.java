package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Sha256Hash;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionOutput;
import org.creativecoinj.crypto.KeyCrypterException;
import org.creativecoinj.wallet.KeyChain;
import org.creativecoinj.wallet.SendRequest;
import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.coin.CoinUtils;
import crea.wallet.lite.ui.main.PrepareTxActivity;
import crea.wallet.lite.util.CoinConverter;
import crea.wallet.lite.util.IntentUtils;
import crea.wallet.lite.util.TxInfo;

import static crea.wallet.lite.application.Constants.WALLET.CONTEXT;
import static crea.wallet.lite.application.WalletApplication.INSTANCE;

public class RBFActivity extends PrepareTxActivity {

    private static final String TAG = "RBFActivity";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String TRANSACTION_FEE = "transaction_fee";

    private Coin feeRaise;
    private EditText feeText;
    private TextView txStatus;
    private TextView txStatusIcon;
    private TextView destinyAddress;
    private TextView destinyAmount;
    private TextView feeAmountBtc;
    private TextView feeAmountFiat;
    private View broadcastStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rbf);


        broadcastStatus = findViewById(R.id.broadcast_status);
        txStatus = (TextView) findViewById(R.id.transaction_status);
        txStatusIcon = (TextView) findViewById(R.id.transaction_status_icon);
        feeAmountBtc = (TextView) findViewById(R.id.fee_amount_btc);
        feeAmountFiat = (TextView) findViewById(R.id.fee_amount_fiat);
        destinyAddress = (TextView) findViewById(R.id.destination_address);
        destinyAmount = (TextView) findViewById(R.id.destination_amount);

        feeRaise = Configuration.getInstance().getTransactionFee();

        feeText = (EditText) findViewById(R.id.new_tx_fee);
        TextView txHashView = (TextView) findViewById(R.id.tx_hash);
        TextView txFee = (TextView) findViewById(R.id.tx_fee);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Sha256Hash txHash;
            if (!TextUtils.isEmpty(extras.getString(TRANSACTION_ID))) {
                txHash = Sha256Hash.wrap(extras.getString(TRANSACTION_ID));
            } else {
                txHash = (Sha256Hash) extras.getSerializable(TRANSACTION_ID);
            }

            if (txHash != null) {
                tx = wallet.getTransaction(txHash);
                txHashView.setText(txHash.toString());
                txFee.setText(tx.getFee().toFriendlyString());
            }


            if (extras.containsKey(TRANSACTION_FEE)) {
                long fee = extras.getLong(TRANSACTION_FEE);
                feeRaise = Coin.valueOf(fee);
            }
        }

        double fee = feeRaise.longValue() / 1e8d;
        feeText.setText(String.valueOf(fee));

        checkValidTransaction();

    }

    private boolean canReplaceTx() {
        return new TxInfo(tx).isReplaceable();
    }

    private void checkValidTransaction() {
        if (tx != null) {
            if (!canReplaceTx()) {
                Toast.makeText(this, "Transaction can not be replace", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Transaction can not be replace", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void requestPin() {
        IntentUtils.checkPin(this);
    }

    private void decryptWallet(final String pin) {
        feeRaise = (Coin) CoinUtils.valueOf("CREA", Double.parseDouble(feeText.getText().toString()));

        new AsyncTask<Void, Void, Void>() {

            Transaction transactionToSend;
            @Override
            protected Void doInBackground(Void... params) {
                org.creativecoinj.core.Context.propagate(CONTEXT);
                CONFIDENCE_LISTENER.onSigning();
                if (wallet.isEncrypted()) {
                    Log.d(TAG, "Decrypting..");
                    wallet.decrypt(pin);
                }

                final TransactionOutput outputSpend = Preconditions.checkNotNull(findSpendableOutput(tx, feeRaise));
                transactionToSend = new Transaction(Constants.WALLET.NETWORK_PARAMETERS);
                transactionToSend.addInput(outputSpend);
                transactionToSend.addOutput(outputSpend.getValue().subtract(feeRaise), wallet.freshAddress(KeyChain.KeyPurpose.CHANGE));
                transactionToSend.setPurpose(Transaction.Purpose.RAISE_FEE);

                final SendRequest sendRequest = SendRequest.forTx(transactionToSend);

                try {
                    Log.d(TAG, "Signing...");
                    wallet.signTransaction(sendRequest);
                    wallet.commitTx(transactionToSend);

                } catch (KeyCrypterException e) {
                    CONFIDENCE_LISTENER.onFailure(e);
                    e.printStackTrace();
                }
                wallet.encrypt(pin);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                CONFIDENCE_LISTENER.onSending();
                INSTANCE.broadcastTransaction(transactionToSend);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private TransactionOutput findSpendableOutput(final Transaction transaction, final Coin minimumOutputValue) {
        for (final TransactionOutput output : transaction.getOutputs()) {
            if (output.isMine(wallet) && output.isAvailableForSpending()
                    && output.getValue().isGreaterThan(minimumOutputValue))
                return output;
        }

        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_seed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_ok:
                requestPin();
                break;
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
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

                        //destinyAddress.setText(getAddress().toString());
                        destinyAmount.setText(totalOutput.toFriendlyString());
                        feeAmountBtc.setText(feeOutput.toFriendlyString());
                        feeAmountFiat.setText(feeConversion.toFriendlyString());

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                String pin = data.getStringExtra(PinActivity.EXTRA_CODE);
                decryptWallet(pin);
            }
        } else {
            Log.e(TAG, "Pin incorrect");
        }
    }
}
