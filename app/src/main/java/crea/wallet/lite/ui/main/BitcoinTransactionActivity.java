package crea.wallet.lite.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import crea.wallet.lite.BookAddress;
import crea.wallet.lite.R;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.ui.base.TransactionActivity;
import com.chip_chap.services.transaction.Btc2BtcTransaction;
import com.chip_chap.services.updater.TransactionUpdaterExecutor;
import com.chip_chap.services.util.ViewUpdater;

import org.creacoinj.core.Coin;

import java.util.ArrayList;

public class BitcoinTransactionActivity extends TransactionActivity<Btc2BtcTransaction> implements ViewUpdater<Btc2BtcTransaction> {

    private static final String TAG = "BTCTransactionActivity";
    private TransactionUpdaterExecutor tUpdaterExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitcoin_transaction);
        setTransactionClass(Btc2BtcTransaction.class);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setFinishOnPause(false);

        TextView addressee = (TextView) findViewById(R.id.addressee);
        TextView sender = (TextView) findViewById(R.id.sender);
        TextView amount = (TextView) findViewById(R.id.amount);
        TextView fee = (TextView) findViewById(R.id.fee);
        TextView txHash = (TextView) findViewById(R.id.tx_hash);
        TextView confirmations = (TextView) findViewById(R.id.confirmations);

        final Btc2BtcTransaction transaction = getTransaction();

        confirmations.setText(String.valueOf(transaction.getConfirmations()));
        Coin coinAmount = Coin.valueOf(transaction.getSatoshis());
        Coin coinFee = Coin.valueOf(transaction.getFee());
        amount.setText(coinAmount.toFriendlyString());
        fee.setText(coinFee.toFriendlyString());

        final String url = Constants.WEB_EXPLORER.BLOCKEXPLORER_URL;

        txHash.setText(Html.fromHtml("<u>"+transaction.getTxHash() + "'</u>"));
        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + "/tx/" + transaction.getTxHash()));
                startActivity(browserIntent);
            }
        });

        String[] origins = transaction.getOrigins();
        String[] destinies = transaction.getDestiny();
        ArrayList<String> resolvedInput = new ArrayList<>();
        ArrayList<String> resolvedOutput = new ArrayList<>();

        for (String o : origins) {
            BookAddress book = BookAddress.resolveAddress(o);
            if (book != null && !TextUtils.isEmpty(book.getLabel())) {
                resolvedInput.add(book.getLabel());
            } else {
                resolvedInput.add(o);
            }
        }

        for (String o : destinies) {
            BookAddress book = BookAddress.resolveAddress(o);
            if (book != null && !TextUtils.isEmpty(book.getLabel())) {
                resolvedOutput.add(book.getLabel());
            } else {
                resolvedOutput.add(o);
            }
        }

        sender.setText(TextUtils.join(", ", resolvedInput));
        addressee.setText(TextUtils.join(", ", resolvedOutput));
    }

    @Override
    public void updateView(final Btc2BtcTransaction btc2BtcTransaction) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView confirmations = (TextView) findViewById(R.id.confirmations);
                confirmations.setText(Integer.toString(btc2BtcTransaction.getConfirmations()));
            }
        });
    }

    @Override
    protected void onPause() {
        tUpdaterExecutor.cancel();
        tUpdaterExecutor = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(tUpdaterExecutor == null) {
            tUpdaterExecutor = TransactionUpdaterExecutor.create(this, 120000L, getTransaction());
            tUpdaterExecutor.start();
        }
    }
}
