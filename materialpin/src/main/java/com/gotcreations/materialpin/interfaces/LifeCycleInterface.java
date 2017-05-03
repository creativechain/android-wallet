package com.gotcreations.materialpin.interfaces;

import android.app.Activity;

/**
 * Created by stoyan on 1/12/15.
 * Allows to follow the LifeCycle of the {@link com.gotcreations.materialpin.PinActivity}
 * Implemented by {@link com.gotcreations.materialpin.managers.AppLockImpl} in order to
 * determine when the app was launched for the last time and when to launch the
 * {@link com.gotcreations.materialpin.managers.AppLockActivity}
 */
public interface LifeCycleInterface {

    /**
     * Called in {@link Activity#onResume()}
     */
    public void onActivityResumed(Activity activity);

    /**
     * Called in {@link Activity#onPause()}
     */
    public void onActivityPaused(Activity activity);
}
