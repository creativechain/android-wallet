package crea.wallet.lite.ui.main;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.R;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.ui.base.TransactionActivity;
import crea.wallet.lite.util.wrapper.TxInfo;

import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Transaction;

import java.util.ArrayList;
import java.util.Arrays;

public class CoinTransactionActivity extends TransactionActivity {

    private static final String TAG = "BTCTransactionActivity";

    public final BroadcastReceiver TX_UPDATE_RECEIVER = new BlockchainBroadcastReceiver() {

        @Override
        public void onLastDownloadedBlock() {
            setConfirmations();
        }
    };

    private TextView confirmations;
    private View rbfDetail;
    private View senderDetail;
    private View addresseeDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitcoin_transaction);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setFinishOnPause(false);

        TextView addressee = (TextView) findViewById(R.id.addressee);
        TextView sender = (TextView) findViewById(R.id.sender);
        TextView amount = (TextView) findViewById(R.id.amount);
        TextView fee = (TextView) findViewById(R.id.fee);
        TextView txHash = (TextView) findViewById(R.id.tx_hash);
        rbfDetail = findViewById(R.id.rbf_detail);
        senderDetail = findViewById(R.id.sender_detail);
        addresseeDetail = findViewById(R.id.addressee_detail);

        confirmations = (TextView) findViewById(R.id.confirmations);

        final Transaction transaction = getTransaction();
        TxInfo txInfo = new TxInfo(transaction);
        confirmations.setText(String.valueOf(transaction.getConfidence().getDepthInBlocks()));

        Coin coinAmount = txInfo.isSentFromUser() ? txInfo.getAmountSend() : txInfo.getAmountReceived();
        Coin coinFee = txInfo.getFee();
        amount.setText(coinAmount.toFriendlyString());
        fee.setText(coinFee.toFriendlyString());

        final String url = Constants.URLS.BLOCKEXPLORER_URL;

        txHash.setText(Html.fromHtml("<u>"+transaction.getHashAsString() + "</u>"));
        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url  + transaction.getHashAsString() + ".htm"));
                startActivity(browserIntent);
            }
        });

        String[] origins = txInfo.getInputAddressesResolved().toArray(new String[]{});
        String[] destinies = txInfo.getOutputAddressesResolved().toArray(new String[]{});
        ArrayList<String> resolvedInput = new ArrayList<>();
        ArrayList<String> resolvedOutput = new ArrayList<>();

        resolvedInput.addAll(Arrays.asList(origins));
        resolvedOutput.addAll(Arrays.asList(destinies));

        sender.setText(TextUtils.join(", ", resolvedInput));
        addressee.setText(TextUtils.join(", ", resolvedOutput));

        if (resolvedInput.isEmpty()) {
            senderDetail.setVisibility(View.GONE);
        }

        if (resolvedOutput.isEmpty()) {
            addresseeDetail.setVisibility(View.GONE);
        }

        if (txInfo.isRBFTransaction()) {
            rbfDetail.setVisibility(View.VISIBLE);
        }
    }

    public void setConfirmations() {
        confirmations.setText(String.valueOf(getTransaction().getConfidence().getDepthInBlocks()));
    }

    @Override
    protected void onPause() {
        unregisterReceiver(TX_UPDATE_RECEIVER);
        super.onPause();
    }

    @Override
    public void onResume() {
        registerReceiver(TX_UPDATE_RECEIVER, new IntentFilter(BlockchainBroadcastReceiver.LAST_BLOCK_RECEIVED));
        super.onResume();
    }
}
