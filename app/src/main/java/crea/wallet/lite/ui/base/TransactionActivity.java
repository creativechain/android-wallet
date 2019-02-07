package crea.wallet.lite.ui.base;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;


import org.creativecoinj.core.Sha256Hash;
import org.creativecoinj.core.Transaction;

import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.wallet.WalletHelper;

/**
 * Created by lluis on 3/22/15.
 */
public abstract class TransactionActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "TransactionActivity";
    public static final String TRANSACTION_ID = "id";

    private Transaction transaction;
    private boolean finishOnPause = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        invalidate();
    }

    public Transaction getTransaction(){
        return this.transaction;
    }

    protected void invalidate(){
        String id = getIntent().getStringExtra(TRANSACTION_ID);
        this.retrieveTransaction(id);
    }

    private void retrieveTransaction(String transactionId){

        if (!TextUtils.isEmpty(transactionId)) {
            transaction = WalletHelper.INSTANCE.getTransaction(Sha256Hash.wrap(transactionId));
        } else {
            throw new IllegalArgumentException("Transaction hash is null "+transactionId);
        }

        if (this.transaction == null) {
            throw new NullPointerException("Transaction is null (Extra ID not provided? id="+transactionId+")");
        }
    }

    @Override
    public void onBackPressed() {
        /*Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);*/
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(finishOnPause) {
            finish();
        }
    }

    public void setFinishOnPause(boolean finishOnPause) {
        this.finishOnPause = finishOnPause;
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    protected void onDestroy() {
        DialogFactory.removeDialogsFrom(getClass());
        super.onDestroy();
    }
}
