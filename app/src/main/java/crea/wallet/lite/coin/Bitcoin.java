package crea.wallet.lite.coin;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.utils.MonetaryFormat;

import java.math.BigDecimal;

/**
 * Created by Andersson G. Acosta on 15/06/17.
 */

public class Bitcoin extends AbstractCoin {

    private static final long serialVersionUID = -1188579869483422890L;
    private static final MonetaryFormat PLAIN_FORMAT = (new MonetaryFormat()).shift(0).minDecimals(2).repeatOptionalDecimals(1, 8).noCode();
    private static final MonetaryFormat FRIENDLY_FORMAT = MonetaryFormat.BTC.minDecimals(2).repeatOptionalDecimals(1, 6)
            .code(0, "BTC").code(1, "mBTC").code(2, "µBTC").postfixCode();

    public Bitcoin(long value) {
        super(value, "BTC", 8);
    }

    public static Bitcoin valueOf(long value) {
        return new Bitcoin(value);
    }

    public static Bitcoin valueOf(String value) {
        try {
            long val = new BigDecimal(value).movePointRight(8).longValue();
            return valueOf(val);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Bitcoin valueOf(int value) {
        return valueOf(Math.round(value * 1E8D));
    }

    public static Bitcoin valueOf(double value) {
        return valueOf(Math.round(value * 1E8D));
    }

    @Override
    public String toPlainString() {
        return PLAIN_FORMAT.format(this).toString();
    }

    @Override
    public String toFriendlyString() {
        return FRIENDLY_FORMAT.format(this).toString();
    }
}
