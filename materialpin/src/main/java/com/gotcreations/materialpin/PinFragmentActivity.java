package com.gotcreations.materialpin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.gotcreations.materialpin.interfaces.LifeCycleInterface;
import com.gotcreations.materialpin.managers.AppLockActivity;

import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Created by stoyan and olivier on 1/12/15.
 * You must extend this Activity in order to support this library.
 * Then to enable PinCode blocking, you must call
 * {@link com.gotcreations.materialpin.managers.LockManager#enableAppLock(Context, Class)}
 */
public class PinFragmentActivity extends FragmentActivity {
    private static LifeCycleInterface mLifeCycleListener;
    private final BroadcastReceiver mPinCancelledReceiver;

    public PinFragmentActivity() {
        super();
        mPinCancelledReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter(AppLockActivity.ACTION_CANCEL);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPinCancelledReceiver, filter);
    }

    @Override
    protected void onResume() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityResumed(PinFragmentActivity.this);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityPaused(PinFragmentActivity.this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPinCancelledReceiver);
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
