package crea.wallet.lite.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.TextView;

import crea.wallet.lite.R;
import crea.wallet.lite.util.wrapper.Typefaces;


/**
 * Created by ander on 10/03/16.
 */
public class FontTextView extends TextView {

    private static final String TAG = "FontTextView";

    public static final int PLAIN = 0;
    public static final int HTML = 1;

    public static final int ROBOTO_LIGHT = 0;
    public static final int ROBOTO_BOLD = 1;
    public static final int ROBOTO_REGULAR = 2;
    public static final int RALEWAY_BOLD = 3;
    public static final int OLIVIER = 4;
    public static final int MATERIAL_ICON = 5;

    private int font;
    private int textType;

    public FontTextView(Context context) {
        super(context);
        setFont(ROBOTO_LIGHT);
    }

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    public FontTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs);
    }

    private void initialize(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FontTextView);

        setFont(a.getInteger(R.styleable.FontTextView_font, ROBOTO_REGULAR));
        setTextType(a.getInteger(R.styleable.FontTextView_text_type, PLAIN));
        a.recycle();
    }

    public void setTextType(int textType) {
        this.textType = textType;
        if (textType == HTML) {
            setText(Html.fromHtml(getText().toString()));
        } else {
            setText(getText());
        }
    }

    public void setFont(int font) {
        this.font = font;
        setTypeface(null);
    }

    public Typeface getTypeFace() {
        switch (font) {
            case RALEWAY_BOLD:
                return Typefaces.get(getContext(), "fonts/raleway_bold.ttf");
            case ROBOTO_BOLD:
                return Typefaces.get(getContext(), "fonts/roboto_bold.ttf");
            case ROBOTO_LIGHT:
                return Typefaces.get(getContext(), "fonts/roboto_light.ttf");
            case OLIVIER:
                return Typefaces.get(getContext(), "fonts/olivier_demo.ttf");
            case MATERIAL_ICON:
                return Typefaces.get(getContext(), "fonts/material-font.ttf");
            default:
                return Typefaces.get(getContext(), "fonts/roboto_regular.ttf");
        }
    }
    @Override
    public void setTypeface(Typeface tf) {
        super.setTypeface(getTypeFace());
    }
}
