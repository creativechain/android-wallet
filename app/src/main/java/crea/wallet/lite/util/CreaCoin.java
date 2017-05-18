package crea.wallet.lite.util;

import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.MonetaryFormat;
import com.chip_chap.services.cash.coin.base.CryptoCoin;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Andersson G. Acosta on 18/05/17.
 */

public class CreaCoin extends CryptoCoin<CreaCoin> {
    
    protected CreaCoin() {
        super(Currency.FAC);
    }

    public CreaCoin add(CreaCoin creaCoin) {
        if(this.isSameCoin(creaCoin)) {
            this.setValue(this.getValue().add(creaCoin.getValue()));
        }

        return this;
    }

    public CreaCoin add(long value) {
        return this.add(valueOf(value));
    }

    public CreaCoin add(double value) {
        return this.add(valueOf(value));
    }

    public CreaCoin subtract(CreaCoin creaCoin) {
        if(this.isSameCoin(creaCoin)) {
            this.setValue(this.getValue().subtract(creaCoin.getValue()));
        }

        return this;
    }

    public CreaCoin subtract(long subtrahend) {
        return this.subtract(valueOf(subtrahend));
    }

    public CreaCoin subtract(double subtrahend) {
        return this.subtract(valueOf(subtrahend));
    }

    public CreaCoin multiply(long multiplicand) {
        BigDecimal mult = BigDecimal.valueOf(multiplicand).setScale(this.getExponent());
        this.setValue(this.getValue().multiply(mult));
        return this;
    }

    public CreaCoin multiply(double multiplicand) {
        BigDecimal mult = BigDecimal.valueOf(multiplicand).setScale(this.getExponent());
        this.setValue(this.getValue().multiply(mult));
        return this;
    }

    public CreaCoin multiply(int multiplicand) {
        return this.multiply(Double.valueOf((double)multiplicand).doubleValue());
    }

    public CreaCoin divide(long multiplicand) {
        BigDecimal mult = BigDecimal.valueOf(multiplicand).setScale(this.getExponent());
        this.setValue(this.getValue().divide(mult));
        return this;
    }

    public CreaCoin divide(double multiplier) {
        BigDecimal mult = BigDecimal.valueOf(multiplier).setScale(this.getExponent());
        this.setValue(this.getValue().divide(mult));
        return this;
    }

    public CreaCoin divide(int multiplicand) {
        return this.divide(Double.valueOf((double)multiplicand).doubleValue());
    }

    public String toPlainString(int unit) {
        switch(unit) {
            case 0:
            case 2:
            case 5:
                return (new MonetaryFormat()).digits(0, unit).format(this.getLongValue(), unit);
            case 1:
            case 3:
            case 4:
            default:
                return this.toPlainString();
        }
    }

    @Override
    public String toFriendlyString(int unit) {
        switch(unit) {
            case 0:
                return this.toPlainString(0);
            case 1:
            case 3:
            case 4:
            default:
                return this.toPlainString();
            case 2:
                return (new MonetaryFormat()).digits(0, unit).format(this.getLongValue(), unit) + " uCREA";
            case 5:
                return (new MonetaryFormat()).digits(0, unit).format(this.getLongValue(), unit) + " mCREA";
        }
    }

    @Override
    public String toFriendlyString(int minDecimals, int maxDecimals) {
        return this.toPlainString(minDecimals, maxDecimals) + " CREA";
    }

    @Override
    public String toFriendlyString() {
        return this.toPlainString() + " CREA";
    }

    public static CreaCoin valueOf(String value) {
        if(value.isEmpty()) {
            throw new RuntimeException("Impossible parse empty value.");
        } else {
            return value.contains(".")?valueOf(Double.parseDouble(value)):valueOf(Long.parseLong(value));
        }
    }

    public static CreaCoin valueOf(long satoshis) {
        return valueOf((double)satoshis / 1.0E8D);
    }

    public static CreaCoin valueOf(int value) {
        return valueOf(Math.round((double)value * 1.0E8D));
    }

    public static CreaCoin valueOf(double value) {
        return valueOf(BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_DOWN));
    }

    public static CreaCoin valueOf(BigDecimal value) {
        CreaCoin creaCoin = new CreaCoin();
        creaCoin.setValue(value);
        return creaCoin;
    }
}
