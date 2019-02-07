package crea.wallet.lite.wallet;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import crea.wallet.lite.R;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.util.task.Task;
import crea.wallet.lite.util.task.TaskExceptionHandler;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.InsufficientMoneyException;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionOutput;
import org.creativecoinj.crypto.KeyCrypterException;
import org.creativecoinj.wallet.SendRequest;
import org.creativecoinj.wallet.Wallet;

import java.util.List;


import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;

/**
 * Created by ander on 28/01/16.
 */
public class PaymentProcess {

    private static final String TAG = "PaymentProcess";

    private WalletHelper walletHelper;
    private Activity activity;
    private SendRequest sendRequest;
    private String key;
    private PaymentProcessListener processListener;
    private Transaction tx;

    public PaymentProcess(Activity activity, SendRequest sendRequest, String key) {
        this.walletHelper = WalletHelper.INSTANCE;
        this.activity = activity;
        this.key = key;
        this.sendRequest = sendRequest;
    }

    public void start() {
        sendMoney();
    }

    public PaymentProcessListener getProcessListener() {
        return processListener;
    }

    public PaymentProcess setProcessListener(PaymentProcessListener processListener) {
        this.processListener = processListener;
        return this;
    }

    public Address getDestiny() {
        Wallet wallet = walletHelper.getWallet();
        List<TransactionOutput> outputList = sendRequest.tx.getOutputs();
        for (TransactionOutput txOut : outputList) {
            if (!txOut.isMine(wallet)) {
                try {
                    return txOut.getAddressFromP2PKHScript(NETWORK_PARAMETERS);
                } catch (Exception e) {
                    return txOut.getAddressFromP2SH(NETWORK_PARAMETERS);
                }
            }
        }

        return null;
    }

    public Coin getAmountToSend() {
        Wallet wallet = walletHelper.getWallet();
        List<TransactionOutput> outputList = sendRequest.tx.getOutputs();
        for (TransactionOutput txOut : outputList) {
            if (!txOut.isMine(wallet)) {
                return txOut.getValue();
            }
        }

        return null;
    }

    public void sendMoney() {
        String addressee = getDestiny().toBase58();

        String message = String.format(getString(R.string.want_to_send_payment),
                getAmountToSend().toFriendlyString(), addressee, sendRequest.tx.getFee().toFriendlyString());

        AlertDialog dialogBuilder = DialogFactory.alert(activity, R.string.new_btc_payment, message);

        dialogBuilder.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialogBuilder.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                processNewSent();
            }
        });
        dialogBuilder.show();
    }

    private void processNewSent() {
        if (processListener != null) {
            processListener.onSigning();
        }

        Task<Exception> task = new Task<Exception>() {
            @Override
            public void doTask(Exception e) {
                if (processListener != null) {
                    if (e != null) {
                        if (e instanceof InsufficientMoneyException) {
                            InsufficientMoneyException i = (InsufficientMoneyException) e;
                            processListener.onInsufficientMoney(i.missing);
                        } else if (e instanceof KeyCrypterException) {
                            e.printStackTrace();
                            processListener.onInvalidEncryptionKey();
                        } else {
                            processListener.onFailure(e);
                        }
                    } else {
                        processListener.onSuccess(tx);
                    }
                }
            }
        };

        new TaskExceptionHandler<Void, Void>(task) {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    WalletHelper.INSTANCE.singTransaction(key, sendRequest);
                    tx = sendRequest.tx;
                    if (processListener != null) {
                        processListener.onSending();
                    }

                    Log.i(TAG, "Broadcasting transaction: " + tx.getHashAsString());
                    WalletApplication.INSTANCE.processDirectTransaction(tx);
                    publishProgress();
                } catch (Exception e)  {
                    publishProgress(e);
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String getString(@StringRes int id) {
        return activity.getString(id);
    }
}
