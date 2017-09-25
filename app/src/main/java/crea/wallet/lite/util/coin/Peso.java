package crea.wallet.lite.util.coin;

import org.creativecoinj.utils.Fiat;

import java.math.BigDecimal;

/**
 * Created by Andersson G. Acosta on 15/06/17.
 */

public class Peso extends Fiat {

    private static final long serialVersionUID = -9013991247367251734L;

    private Peso(long value) {
        super("MXN", value);
    }

    public static Peso valueOf(long value) {
        return new Peso(value);
    }

    public static Peso valueOf(String value) {
        try {
            long val = new BigDecimal(value).movePointRight(4).longValue();
            return valueOf(val);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Peso valueOf(int value) {
        return valueOf(Math.round(value * 1E4D));
    }

    public static Peso valueOf(double value) {
        return valueOf(Math.round(value * 1E4D));
    }
}
