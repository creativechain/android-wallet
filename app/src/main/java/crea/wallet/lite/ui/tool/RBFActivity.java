package crea.wallet.lite.ui.tool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import org.creativecoinj.core.Sha256Hash;

import crea.wallet.lite.R;
import crea.wallet.lite.ui.main.PrepareTxActivity;

public class RBFActivity extends PrepareTxActivity {

    public static final String TRANSACTION_ID = "transaction_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rbf);

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
            }
        }

        checkValidTransaction();

    }

    private boolean canReplaceTx() {
        return tx.isOptInFullRBF() && tx.getConfidence().getDepthInBlocks() >= 1;
    }

    private void checkValidTransaction() {
        if (tx != null) {
            if (canReplaceTx()) {

            } else {
                Toast.makeText(this, "Transaction can not be replace", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Transaction can not be replace", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onTransactionState(State state) {

    }
}
