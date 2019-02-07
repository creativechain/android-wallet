package crea.wallet.lite.util.wrapper;

import androidx.annotation.NonNull;

import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.util.coin.CoinUtils;

import org.creativecoinj.core.AbstractCoin;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ander on 16/02/16.
 */
public class Prices {

    private static final String TAG = "Prices";

    private static Map<String, Long> coinPrices = new HashMap<>();

    public static void setPrice(@NonNull AbstractCoin price) {
        coinPrices.put(price.getCurrencyCode(), price.getValue());
        Configuration conf = Configuration.getInstance();
        conf.setCreaPrice(price.getCurrencyCode(), price.getValue());
    }

    public static AbstractCoin getPrice(String c) {
        c = c.toUpperCase();
        long price = coinPrices.get(c);
        if (price > 0) {
            return CoinUtils.valueOf(c, price);
        } else {
            Configuration conf = Configuration.getInstance();
            return conf.getCreaPrice(c);
        }

    }

}
