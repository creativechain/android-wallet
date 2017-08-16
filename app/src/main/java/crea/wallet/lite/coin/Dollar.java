package crea.wallet.lite.coin;

import android.text.TextUtils;

import org.creativecoinj.utils.Fiat;

import java.math.BigDecimal;

/**
 * Created by Andersson G. Acosta on 15/06/17.
 */

public class Dollar extends Fiat {

    private static final long serialVersionUID = 6485934383907706953L;

    private Dollar(long value) {
        super("$", value);
    }

    public static Dollar valueOf(long value) {
        return new Dollar(value);
    }

    public static Dollar valueOf(String value) {
        try {
            long val = new BigDecimal(value).movePointRight(4).longValue();
            return valueOf(val);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Dollar valueOf(int value) {
        return valueOf(Math.round(value * 1E4D));
    }

    public static Dollar valueOf(double value) {
        return valueOf(Math.round(value * 1E4D));
    }
}
