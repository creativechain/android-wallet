package crea.wallet.lite.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import crea.wallet.lite.util.wrapper.Typefaces;

/**
 * Created by ander on 10/03/16.
 */
public class MaterialTextView extends TextView {

    private static final String TAG = "MaterialTextView";

    public MaterialTextView(Context context) {
        super(context);
    }

    public MaterialTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaterialTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Typeface getTypeFace() {
        return Typefaces.get(getContext(), "fonts/material-font.ttf");
    }

    @Override
    public void setTypeface(Typeface tf) {
        super.setTypeface(getTypeFace());
    }
}
