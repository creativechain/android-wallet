package com.gotcreations.materialpin.views;

import android.support.v7.app.ActionBarActivity;

import com.gotcreations.materialpin.interfaces.LifeCycleInterface;

/**
 * Created by stoyan and olivier on 1/12/15.
 * You must extend this Activity in order to support this library.
 * Then to enable PinCode blocking, you must call
 * {@link com.gotcreations.materialpin.managers.LockManager#enableAppLock(android.content.Context, Class)}
 */
public class PinActionBarActivity extends ActionBarActivity {
    private static LifeCycleInterface mLifeCycleListener;

    @Override
    protected void onResume() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityResumed(PinActionBarActivity.this);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityPaused(PinActionBarActivity.this);
        }
        super.onPause();
    }

    public static void setListener(LifeCycleInterface listener) {
        if (mLifeCycleListener != null) {
            mLifeCycleListener = null;
        }
        mLifeCycleListener = listener;
    }

    public static void clearListeners() {
        mLifeCycleListener = null;
    }

    public static boolean hasListeners() {
        return (mLifeCycleListener != null);
    }
}
