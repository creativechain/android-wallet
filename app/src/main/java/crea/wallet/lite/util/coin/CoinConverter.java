package crea.wallet.lite.util.coin;

import android.util.Log;

import org.creativecoinj.core.AbstractCoin;

import java.math.BigDecimal;

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
        Log.e(TAG, "AmountToConvert: " + amount.toFriendlyString());
        calculate();
        return this;
    }

    public CoinConverter price(AbstractCoin price) {
        this.price = price;
        Log.e(TAG, "Price: " + price.toFriendlyString());
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

    public String toString(boolean withCurrency) {
        if (amountConverted == null) {
            return "0.00";
        }

        if (withCurrency) {
            return amountConverted.toFriendlyString();
        }

        return amountConverted.toPlainString();
    }

    @Override
    public String toString() {
        return toString(true);
    }
}
