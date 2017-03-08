package crea.wallet.lite.wallet;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.util.DialogFactory;
import crea.wallet.lite.util.Utils;
import com.chip_chap.services.task.Task;
import com.chip_chap.services.task.TaskExceptionHandler;

import org.creacoinj.core.Address;
import org.creacoinj.core.Coin;
import org.creacoinj.core.InsufficientMoneyException;
import org.creacoinj.core.Transaction;
import org.creacoinj.crypto.KeyCrypterException;
import org.creacoinj.wallet.SendRequest;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by ander on 28/01/16.
 */
public class PaymentProcess {

    private static final String TAG = "PaymentProcess";

    private WalletHelper walletHelper;
    private Activity activity;
    private Address address;
    private Coin amountToSent;
    private boolean emptyWallet = false;
    private File walletFile;
    private PaymentProcessListener processListener;
    private Transaction tx;

    public PaymentProcess(Activity activity, Address address, Coin amountToSent, File walletFile) {
        this.walletHelper = WalletHelper.INSTANCE;
        this.activity = activity;
        this.address = address;
        this.amountToSent = amountToSent;
        this.walletFile = walletFile;
    }

    public PaymentProcess(Activity activity, Address address, boolean emptyWallet, File walletFile) {
        this.activity = activity;
        this.address = address;
        this.emptyWallet = emptyWallet;
        this.walletFile = walletFile;
        this.walletHelper = WalletHelper.INSTANCE;
    }

    public void start(String key) {
        sendMoney(key, emptyWallet ? 0 : amountToSent.longValue(), address);
    }

    public PaymentProcessListener getProcessListener() {
        return processListener;
    }

    public PaymentProcess setProcessListener(PaymentProcessListener processListener) {
        this.processListener = processListener;
        return this;
    }

    public void sendMoney(final String key, long satoshis, final Address address) {

        Coin balance = walletHelper.getMainBalance();
        final Coin amountToSend = Coin.valueOf(satoshis);
        FeeCalculation feeCalculation;
        if (emptyWallet) {
            feeCalculation = new FeeCalculation(WalletHelper.INSTANCE.getWallet(walletFile), address);
        } else {
            feeCalculation = new FeeCalculation(WalletHelper.INSTANCE.getWallet(walletFile), address, amountToSend);
        }

        Log.i(TAG, "Fee: " + feeCalculation.getTxFee().toFriendlyString() + ", Total: " + feeCalculation.getTotalToSent().toFriendlyString());
        Log.i(TAG, "Sufficient Money: " + feeCalculation.hasSufficientMoney());
        Log.i(TAG, "current balance: " + balance.getValue());
        Log.i(TAG, "to send: " + amountToSend.getValue());

        if (feeCalculation.hasSufficientMoney()) {
            String addressee = address.toString();

            String message = String.format(getString(R.string.want_to_send_payment),
                    emptyWallet ? feeCalculation.getTotalToSent().toFriendlyString() : amountToSend.toFriendlyString(),
                    addressee, feeCalculation.getTxFee().toFriendlyString());

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
                    Log.i(TAG, "Sending " + amountToSend.toFriendlyString());
                    processNewSent(key, amountToSend, address);
                }
            });
            dialogBuilder.show();
        } else {
            Coin missing = feeCalculation.getMissing();
            if (processListener != null) {
                processListener.onInsufficientMoney(missing);
            }
        }
    }

    private void processNewSent(final String key, final Coin amountToSent, final Address address) {
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
                String k = Configuration.getInstance().getPin();
                try {
                    k = Utils.encryptInSHA2(k, 3);

                    final SendRequest sReq = WalletHelper.INSTANCE.prepareTransaction(walletFile, k, k, amountToSent, address, emptyWallet);
                    tx = sReq.tx;
                    if (processListener != null) {
                        processListener.onSending();
                    }

                    WalletHelper.INSTANCE.commitTx(walletFile, sReq);
                    WalletApplication.INSTANCE.processDirectTransaction(tx);
                    Log.i(TAG, "Broadcasting transaction: " + tx.getHashAsString());
                    publishProgress();
                } catch (InsufficientMoneyException | KeyCrypterException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException e)  {
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
