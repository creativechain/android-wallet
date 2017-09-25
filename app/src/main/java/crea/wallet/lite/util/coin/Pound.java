package crea.wallet.lite.util.coin;

import org.creativecoinj.utils.Fiat;

import java.math.BigDecimal;

/**
 * Created by Andersson G. Acosta on 15/06/17.
 */

public class Pound extends Fiat {

    private static final long serialVersionUID = -2735188555928641041L;

    private Pound(long value) {
        super("£", value);
    }

    public static Pound valueOf(long value) {
        return new Pound(value);
    }

    public static Pound valueOf(String value) {
        try {
            long val = new BigDecimal(value).movePointRight(4).longValue();
            return valueOf(val);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Pound valueOf(int value) {
        return valueOf(Math.round(value * 1E4D));
    }

    public static Pound valueOf(double value) {
        return valueOf(Math.round(value * 1E4D));
    }
}
