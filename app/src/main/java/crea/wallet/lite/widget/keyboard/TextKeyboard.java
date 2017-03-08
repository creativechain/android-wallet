package crea.wallet.lite.widget.keyboard;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import crea.wallet.lite.R;

/**
 * Created by ander on 16/03/16.
 */
public class TextKeyboard extends LinearLayout implements View.OnClickListener {

    private static final String TAG = "TextKeyboard";

    private static final String[] KEYS_NAMES = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
            "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "erase", "ok"};

    private OnButtonPressedListener mOnButtonPressedListener;
    private int letter = OnButtonPressedListener.KEY_UNKNOWN;

    public TextKeyboard(Context context) {
        super(context);
        initialize();
    }

    public TextKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.text_keyboard, this);
        LinearLayout root = (LinearLayout) findViewById(R.id.keyboard);
        root.setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        for (String s : KEYS_NAMES) {
            String keyName = "key_" + s;
            View v = findViewByName(keyName);
            if (v != null) {
                v.setOnClickListener(this);
            }

        }
    }

    private View findViewByName(String name) {
        int id = getResources().getIdentifier(name, "id", getContext().getPackageName());
        return findViewById(id);
    }

    public void setOnButtonPressedListener(OnButtonPressedListener onButtonPressedListener) {
        this.mOnButtonPressedListener = onButtonPressedListener;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.key_a:
                letter = OnButtonPressedListener.KEY_A;
                break;
            case R.id.key_b:
                letter = OnButtonPressedListener.KEY_B;
                break;
            case R.id.key_c:
                letter = OnButtonPressedListener.KEY_C;
                break;
            case R.id.key_d:
                letter = OnButtonPressedListener.KEY_D;
                break;
            case R.id.key_e:
                letter = OnButtonPressedListener.KEY_E;
                break;
            case R.id.key_f:
                letter = OnButtonPressedListener.KEY_F;
                break;
            case R.id.key_g:
                letter = OnButtonPressedListener.KEY_G;
                break;
            case R.id.key_h:
                letter = OnButtonPressedListener.KEY_H;
                break;
            case R.id.key_i:
                letter = OnButtonPressedListener.KEY_I;
                break;
            case R.id.key_j:
                letter = OnButtonPressedListener.KEY_J;
                break;
            case R.id.key_k:
                letter = OnButtonPressedListener.KEY_K;
                break;
            case R.id.key_l:
                letter = OnButtonPressedListener.KEY_L;
                break;
            case R.id.key_m:
                letter = OnButtonPressedListener.KEY_M;
                break;
            case R.id.key_n:
                letter = OnButtonPressedListener.KEY_N;
                break;
            case R.id.key_o:
                letter = OnButtonPressedListener.KEY_O;
                break;
            case R.id.key_p:
                letter = OnButtonPressedListener.KEY_P;
                break;
            case R.id.key_q:
                letter = OnButtonPressedListener.KEY_Q;
                break;
            case R.id.key_r:
                letter = OnButtonPressedListener.KEY_R;
                break;
            case R.id.key_s:
                letter = OnButtonPressedListener.KEY_S;
                break;
            case R.id.key_t:
                letter = OnButtonPressedListener.KEY_T;
                break;
            case R.id.key_u:
                letter = OnButtonPressedListener.KEY_U;
                break;
            case R.id.key_v:
                letter = OnButtonPressedListener.KEY_V;
                break;
            case R.id.key_w:
                letter = OnButtonPressedListener.KEY_W;
                break;
            case R.id.key_x:
                letter = OnButtonPressedListener.KEY_X;
                break;
            case R.id.key_y:
                letter = OnButtonPressedListener.KEY_Y;
                break;
            case R.id.key_z:
                letter = OnButtonPressedListener.KEY_Z;
                break;
            case R.id.key_ok:
                letter = OnButtonPressedListener.OK_KEY;
                break;
            case R.id.key_erase:
                letter = OnButtonPressedListener.DELETE_KEY;
                break;
            default:
                letter = OnButtonPressedListener.KEY_UNKNOWN;
                break;

        }

        if (mOnButtonPressedListener != null) {
            mOnButtonPressedListener.onButtonPressed(letter);
        }
    }

    public String letterToString() {
        return keyToString(letter);
    }

    public static String keyToString(int letter) {
        if (letter == OnButtonPressedListener.DELETE_KEY) {
            return "erase";
        } else if (letter == OnButtonPressedListener.OK_KEY) {
            return "ok";
        } else if (letter == OnButtonPressedListener.NEXT_KEY) {
            return "next";
        } else if (letter <= OnButtonPressedListener.KEY_UNKNOWN || letter > OnButtonPressedListener.KEY_Z) {
            return "unknown";
        }

        return KEYS_NAMES[letter - OnButtonPressedListener.KEY_A];
    }
}
