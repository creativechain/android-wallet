package com.gotcreations.materialpin.interfaces;

import com.gotcreations.materialpin.enums.KeyboardButtonEnum;

/**
 * Created by stoyan and oliviergoutay on 1/13/15.
 * The {@link com.gotcreations.materialpin.managers.AppLockActivity} will implement
 * this in order to receive events from {@link com.gotcreations.materialpin.views.KeyboardButtonView}
 * and {@link com.gotcreations.materialpin.views.KeyboardView}
 */
public interface KeyboardButtonClickedListener {

    /**
     * Receive the click of a button, just after a {@link android.view.View.OnClickListener} has fired.
     * Called before {@link #onRippleAnimationEnd()}.
     * @param keyboardButtonEnum The organized enum of the clicked button
     */
    public void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum);

    /**
     * Receive the end of a {@link com.andexert.library.RippleView} animation using a
     * {@link com.andexert.library.RippleAnimationListener} to determine the end.
     * Called after {@link #onKeyboardClick(com.gotcreations.materialpin.enums.KeyboardButtonEnum)}.
     */
    public void onRippleAnimationEnd();

}
