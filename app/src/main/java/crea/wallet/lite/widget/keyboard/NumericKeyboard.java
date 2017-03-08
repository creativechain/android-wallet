package crea.wallet.lite.widget.keyboard;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import crea.wallet.lite.R;

/**
 * Created by ander on 11/06/15.
 */
public class NumericKeyboard extends LinearLayout implements View.OnClickListener{

    private static final String TAG = "NumericKeyboard";

    private Context context;
    private OnButtonPressedListener mOnButtonPressedListener;
    private Button button0;
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5;
    private Button button6;
    private Button button7;
    private Button button8;
    private Button button9;
    private ImageButton button10;
    private ImageButton button11;

    public NumericKeyboard(Context context) {
        super(context);
        this.context = context;
        initialize();
    }

    public NumericKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initialize();
    }

    private void initialize() {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.number_keyboard2, this);
        LinearLayout root = (LinearLayout) findViewById(R.id.keyboard);
        root.setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        button0 = (Button) findViewById(R.id.button0);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.button5);
        button6 = (Button) findViewById(R.id.button6);
        button7 = (Button) findViewById(R.id.button7);
        button8 = (Button) findViewById(R.id.button8);
        button9 = (Button) findViewById(R.id.button9);
        button10 = (ImageButton) findViewById(R.id.button_unknown);
        button11 = (ImageButton) findViewById(R.id.button_del);

        button0.setOnClickListener(this);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
        button5.setOnClickListener(this);
        button6.setOnClickListener(this);
        button7.setOnClickListener(this);
        button8.setOnClickListener(this);
        button9.setOnClickListener(this);
        button10.setOnClickListener(this);
        button11.setOnClickListener(this);

    }

    public void setOnButtonPressedListener(OnButtonPressedListener listener) {
        this.mOnButtonPressedListener = listener;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        int digit = -2;
        if (id == button0.getId()) {
            digit = OnButtonPressedListener.KEY_0;
        } else if (id == button1.getId()) {
            digit = OnButtonPressedListener.KEY_1;
        } else if (id == button2.getId()) {
            digit = OnButtonPressedListener.KEY_2;
        } else if (id == button3.getId()) {
            digit = OnButtonPressedListener.KEY_3;
        } else if (id == button4.getId()) {
            digit = OnButtonPressedListener.KEY_4;
        } else if (id == button5.getId()) {
            digit = OnButtonPressedListener.KEY_5;
        } else if (id == button6.getId()) {
            digit = OnButtonPressedListener.KEY_6;
        } else if (id == button7.getId()) {
            digit = OnButtonPressedListener.KEY_7;
        } else if (id == button8.getId()) {
            digit = OnButtonPressedListener.KEY_8;
        } else if (id == button9.getId()) {
            digit = OnButtonPressedListener.KEY_9;
        } else if (id == button10.getId()) {
            digit = OnButtonPressedListener.NEXT_KEY;
        } else if (id == button11.getId()) {
            digit = OnButtonPressedListener.DELETE_KEY;
        }

        if (mOnButtonPressedListener != null) {
            mOnButtonPressedListener.onButtonPressed(digit);
        }
    }
}
