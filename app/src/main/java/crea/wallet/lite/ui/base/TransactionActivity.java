package crea.wallet.lite.ui.base;

import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.activeandroid.query.Select;
import crea.wallet.lite.util.DialogFactory;
import com.chip_chap.services.status.ServiceStatus;
import com.chip_chap.services.status.TransactionStatus;
import com.chip_chap.services.transaction.ChipChapTransaction;
import com.chip_chap.services.updater.ServiceStatusUpdater;
import com.chip_chap.services.updater.TransactionUpdaterExecutor;
import com.chip_chap.services.util.ViewUpdater;

/**
 * Created by lluis on 3/22/15.
 */
public abstract class TransactionActivity<Transaction extends ChipChapTransaction> extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "TransactionActivity";
    public static final String FB_PACKAGE = "com.facebook.katana";
    public static final String TW_PACKAGE = "com.twitter.android";
    public static final String TRANSACTION_ID = "id";

    private Transaction transaction;
    private Class<Transaction> transactionClass;
    private TransactionUpdaterExecutor transactionUpdater;
    private ServiceStatusUpdater serviceStatusUpdater;
    private ViewUpdater<Transaction> viewUpdater;
    private boolean finishOnPause = true;

    public Transaction getTransaction(){
        return this.transaction;
    }

    protected void setTransactionClass(Class<Transaction> transactionClass) {
        this.transactionClass = transactionClass;
        invalidate();
    }

    protected void invalidate(){
        long id = getIntent().getLongExtra("id", 0);
        this.retrieveTransaction(id);
    }

    private void retrieveTransaction(long transactionId){

        if (transactionClass == null) {
            throw new NullPointerException("Transaction class is null");
        }
        this.transaction = new Select()
                .from(transactionClass)
                .where("id = ?", transactionId)
                .executeSingle();

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

    protected void startTransactionUpdater(ViewUpdater<Transaction> viewUpdater) {
        startTransactionUpdater(viewUpdater, 15000L);
    }

    protected void startTransactionUpdater(ViewUpdater<Transaction> viewUpdater, long interval) {
        if (transactionUpdater == null) {
            this.viewUpdater = viewUpdater;
            transactionUpdater = TransactionUpdaterExecutor.create(viewUpdater, interval, getTransaction());
            transactionUpdater.start();
        }
    }

    protected void stopTransactionUpdater() {
        if (transactionUpdater != null) {
            transactionUpdater.cancel();
            transactionUpdater = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewUpdater != null) {
            startTransactionUpdater(viewUpdater);
        }
    }

    @Override
    protected void onPause() {
        stopTransactionUpdater();
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

    public boolean isOk() {
        return !getTransaction().isStatusEquals(TransactionStatus.ERROR, TransactionStatus.EXPIRED);
    }

    @Override
    protected void onDestroy() {
        DialogFactory.removeDialogsFrom(getClass());
        super.onDestroy();
    }

    public void onServiceStatus(ServiceStatus status) {

    }
}
