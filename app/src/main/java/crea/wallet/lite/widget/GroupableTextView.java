package crea.wallet.lite.widget;

import android.content.Context;
import android.util.AttributeSet;

import crea.wallet.lite.wallet.WalletUtils;

/**
 * Created by ander on 17/11/16.
 */
public class GroupableTextView extends FontTextView{

    private static final String TAG = "AddressTextView";

    private int groupLength = 4;

    public GroupableTextView(Context context) {
        super(context);
    }

    public GroupableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GroupableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {

        text = WalletUtils.formatHash(text.toString(), groupLength, 4);
        super.setText(text, type);
    }
}
