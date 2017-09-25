package crea.wallet.lite.util.coin;

import org.creativecoinj.utils.Fiat;

import java.math.BigDecimal;

/**
 * Created by Andersson G. Acosta on 15/06/17.
 */

public class Euro extends Fiat {

    private static final long serialVersionUID = 1178945626711096393L;

    private Euro(long value) {
        super("€", value);
    }

    public static Euro valueOf(long value) {
        return new Euro(value);
    }

    public static Euro valueOf(String value) {
        try {
            long val = new BigDecimal(value).movePointRight(4).longValue();
            return valueOf(val);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Euro valueOf(int value) {
        return valueOf(Math.round(value * 1E4D));
    }

    public static Euro valueOf(double value) {
        return valueOf(Math.round(value * 1E4D));
    }
}
