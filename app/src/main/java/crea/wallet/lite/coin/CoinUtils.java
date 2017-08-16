package crea.wallet.lite.coin;

import android.text.TextUtils;
import android.util.Log;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Coin;

/**
 * Created by Andersson G. Acosta on 20/06/17.
 */

public class CoinUtils {

    private static final String TAG = "CoinUtils";

    public static AbstractCoin valueOf(String currencyCode, long value) {
        if (!TextUtils.isEmpty(currencyCode)) {
            currencyCode = currencyCode.toUpperCase();
            switch (currencyCode) {
                case "CREA":
                    return Coin.valueOf(value);
                case "EUR":
                case "€":
                    return Euro.valueOf(value);
                case "USD":
                case "$":
                    return Dollar.valueOf(value);
                case "MXN":
                    return Peso.valueOf(value);
                case "GBP":
                case "£":
                    return Pound.valueOf(value);
                case "BTC":
                    return Bitcoin.valueOf(value);
                default:
                    throw  new IllegalArgumentException("Invalid currency code: " + currencyCode);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static AbstractCoin valueOf(String currencyCode, double value) {
        if (!TextUtils.isEmpty(currencyCode)) {
            currencyCode = currencyCode.toUpperCase();

            switch (currencyCode) {
                case "CREA":
                    return Coin.valueOf(Math.round(value * 1e8d));
                case "EUR":
                case "€":
                    return Euro.valueOf(value);
                case "USD":
                case "$":
                    return Dollar.valueOf(value);
                case "MXN":
                    return Peso.valueOf(value);
                case "GBP":
                case "£":
                    return Pound.valueOf(value);
                case "BTC":
                    return Bitcoin.valueOf(value);
                default:
                    throw new IllegalArgumentException("Invalid currency code: " + currencyCode);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static AbstractCoin valueOf(String currencyCode, int value) {
        if (!TextUtils.isEmpty(currencyCode)) {
            currencyCode = currencyCode.toUpperCase();
            switch (currencyCode) {
                case "CREA":
                    return Coin.valueOf(value, 0);
                case "EUR":
                case "€":
                    return Euro.valueOf(value);
                case "USD":
                case "$":
                    return Dollar.valueOf(value);
                case "MXN":
                    return Peso.valueOf(value);
                case "GBP":
                case "£":
                    return Pound.valueOf(value);
                case "BTC":
                    return Bitcoin.valueOf(value);
                default:
                    throw new IllegalArgumentException("Invalid currency code: " + currencyCode);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
