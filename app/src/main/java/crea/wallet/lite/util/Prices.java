package crea.wallet.lite.util;

import android.support.annotation.NonNull;

import crea.wallet.lite.application.Configuration;
import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.coin.base.Coin;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ander on 16/02/16.
 */
public class Prices {

    private static final String TAG = "Prices";

    private static Map<Currency, Long> coinPrices = new HashMap<>();

    public static void setPrice(@NonNull Coin price) {
        coinPrices.put(price.getCurrency(), price.getLongValue());
        Configuration conf = Configuration.getInstance();
        conf.setCreaPrice(price.getCurrency(), price.getLongValue());
    }

    public static Coin getPrice(Currency c) {
        long price = coinPrices.get(c);
        if (price > 0) {
            return Coin.fromCurrency(c, price);
        } else {
            Configuration conf = Configuration.getInstance();
            return conf.getCreaPrice(c);
        }

    }

}
