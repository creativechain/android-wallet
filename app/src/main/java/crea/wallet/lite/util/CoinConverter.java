package crea.wallet.lite.util;

import android.util.Log;

import com.google.common.math.LongMath;

import org.creativecoinj.core.AbstractCoin;

import java.math.BigDecimal;
import java.math.BigInteger;

import crea.wallet.lite.coin.CoinUtils;

/**
 * Created by ander on 21/01/16.
 */
public class CoinConverter {

    private static final String TAG = "CoinConverter";

    private AbstractCoin price;
    private AbstractCoin amountToConvert;
    private AbstractCoin amountConverted;

    public CoinConverter amount(AbstractCoin amount) {
        amountToConvert = amount;
        calculate();
        return this;
    }

    public CoinConverter price(AbstractCoin price) {
        this.price = price;
        this.amountConverted = CoinUtils.valueOf(price.getCurrencyCode(), 0);
        calculate();
        return this;
    }

    private void calculate() {

        // CREA      FIAT
        // 1 ------- PRICE
        // A ------- ?


        if (price != null && amountToConvert != null && !price.isZero()) {
            BigDecimal amount = BigDecimal.valueOf(amountToConvert.getValue(), amountToConvert.smallestUnitExponent());
            BigDecimal priceAmount = BigDecimal.valueOf(price.getValue(), price.smallestUnitExponent());
            BigDecimal converted = amount.multiply(priceAmount);


            Log.d(TAG, converted.toString());
            amountConverted = CoinUtils.valueOf(price.getCurrencyCode(), converted.doubleValue());
        }
    }

    public AbstractCoin getConversion() {
        return amountConverted;
    }

    @Override
    public String toString() {
        if (amountConverted == null) {
            return "0.00";
        }

        return amountConverted.toFriendlyString();
    }
}
