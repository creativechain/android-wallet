package crea.wallet.lite.background;

import android.os.Handler;

/**
 * Created by ander on 16/02/16.
 */
public abstract class Updater {

    private static final String TAG = "Updater";

    protected Handler handler;
    protected boolean stopped = false;

    protected Updater() {
        handler = new Handler();
    }

    public abstract void start();

    protected void start(Runnable r) {
        handler.post(r);
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);
    }
}
