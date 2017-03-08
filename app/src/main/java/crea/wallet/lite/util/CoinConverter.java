package crea.wallet.lite.util;

import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.coin.base.Coin;

/**
 * Created by ander on 21/01/16.
 */
public class CoinConverter {

    private static final String TAG = "CoinConverter";

    private Coin price;
    private Coin amountToConvert;
    private Coin amountConverted;

    public CoinConverter amount(Coin amount) {
        amountToConvert = amount;
        calculate();
        return this;
    }
    
    public CoinConverter amount(long amount, Currency currency) {
        return amount(Coin.fromCurrency(currency, amount));
    }

    public CoinConverter amount(int amount, Currency currency) {
        return amount(Coin.fromCurrency(currency, amount));
    }

    public CoinConverter amount(double amount, Currency currency) {
        return amount(Coin.fromCurrency(currency, amount));
    }

    public CoinConverter price(Coin price) {
        this.price = price;
        calculate();
        return this;
    }

    public CoinConverter price(long price, Currency currency) {
        return price(Coin.fromCurrency(currency, price));
    }

    public CoinConverter price(int price, Currency currency) {
        return price(Coin.fromCurrency(currency, price));
    }

    public CoinConverter price(double price, Currency currency) {
        return price(Coin.fromCurrency(currency, price));
    }

    private void calculate() {

        // €       BTC
        // 100 ----- X
        // 1 ------- PRICE

        if (price != null  && amountToConvert != null) {
            double converted = amountToConvert.getDoubleValue() * price.getDoubleValue();
            amountConverted = Coin.fromCurrency(price.getCurrency(), converted);
        }
    }

    public Coin getConversion() {
        return amountConverted;
    }

    @Override
    public String toString() {
        if (amountConverted == null) {
            return "0.00";
        }

        return amountConverted.toPlainString();
    }
}
