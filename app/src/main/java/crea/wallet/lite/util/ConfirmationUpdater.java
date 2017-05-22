package crea.wallet.lite.util;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.chip_chap.services.asynchttp.net.ApiRequester;
import com.chip_chap.services.asynchttp.net.handler.SilentApiHandler;
import com.chip_chap.services.asynchttp.net.util.ApiRequest;
import com.chip_chap.services.calls.base.BaseGetApiRequest;
import com.chip_chap.services.cash.coin.BitCoin;
import com.chip_chap.services.transaction.Btc2BtcTransaction;
import com.chip_chap.services.transaction.ChipChapTransaction;
import com.chip_chap.services.updater.Btc2BtcTransactionUpdater;
import com.chip_chap.services.updater.TransactionUpdaterExecutor;
import com.chip_chap.services.updater.UpdaterLauncher;
import com.chip_chap.services.util.ViewUpdater;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import crea.wallet.lite.application.Constants;

/**
 * Created by Andersson G. Acosta on 21/03/17.
 */
public class ConfirmationUpdater implements Runnable {

    private static final String TAG = "ConfirmationUpdater";

    public static final long DEFAULT_UPDATE_INTERVAL = 60000L;

    private Handler handler;
    private boolean stopped = false;
    private List<Btc2BtcTransaction> transactions;
    private long updateInterval = 60000L;
    private ViewUpdater<Btc2BtcTransaction> viewHolder;

    private ConfirmationUpdater(ViewUpdater<Btc2BtcTransaction> viewHolder, List<Btc2BtcTransaction> transactions) {
        this.viewHolder = viewHolder;
        this.transactions = transactions;
        this.handler = new Handler();
    }

    public void cancel() {
        this.stopped = true;
        this.handler.removeCallbacksAndMessages((Object)null);
        this.viewHolder = null;
    }

    private void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public long getUpdateInterval() {
        return this.updateInterval;
    }

    public void start() {
        this.handler.post(this);
    }

    public void run() {
        if(!this.stopped) {

            for (final Btc2BtcTransaction tx : transactions) {
                ApiRequest confRequest = new BaseGetApiRequest() {
                    @Override
                    public String getURL() {
                        return Constants.WEB_EXPLORER.BLOCKEXPLORER_PROD_URL + tx.getTxHash() + "&decrypt=1";
                    }
                };

                new ApiRequester(confRequest, new SilentApiHandler() {
                    @Override
                    public void onOkResponse(JSONObject jsonObject) throws JSONException {
                        int conf = 0;
                        if(jsonObject.has("confirmations")) {
                            Log.d(TAG, "Has confirmations");
                            conf = jsonObject.getInt("confirmations");
                        }

                        tx.setConfirmations(conf);
                        tx.save();
                        if (viewHolder != null) {
                            viewHolder.updateView(tx);
                        }
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                this.handler.postDelayed(this, this.updateInterval);
            }
        }

    }

    public boolean isRunning() {
        return !this.stopped;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public static ConfirmationUpdater create(ViewUpdater<Btc2BtcTransaction> viewHolder, List<Btc2BtcTransaction> transactions) {
        return new ConfirmationUpdater(viewHolder, transactions);
    }

    public static ConfirmationUpdater create(ViewUpdater<Btc2BtcTransaction> viewHolder, Btc2BtcTransaction... transactions) {
        ConfirmationUpdater tue = create(viewHolder, Arrays.asList(transactions));
        return tue;
    }

    public static ConfirmationUpdater create(ViewUpdater<Btc2BtcTransaction> viewHolder, List<Btc2BtcTransaction> transactions, long updateInterval) {
        ConfirmationUpdater tue = create(viewHolder, transactions);
        tue.setUpdateInterval(updateInterval);
        return tue;
    }

    public static ConfirmationUpdater create(ViewUpdater<Btc2BtcTransaction> viewHolder, long updateInterval, Btc2BtcTransaction... transactions) {
        ConfirmationUpdater tue = create(viewHolder, Arrays.asList(transactions), updateInterval);
        return tue;
    }
}
