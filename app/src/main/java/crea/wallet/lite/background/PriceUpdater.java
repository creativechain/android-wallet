package crea.wallet.lite.background;

import android.os.AsyncTask;

import crea.wallet.lite.util.Prices;
import com.chip_chap.services.asynchttp.net.ApiRequester;
import com.chip_chap.services.asynchttp.net.handler.SilentApiHandler;
import com.chip_chap.services.calls.request.ExchangeApiRequest;
import com.chip_chap.services.cash.Currency;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by ander on 16/02/16.
 */
public class PriceUpdater extends Updater {

    private static final String TAG = "PriceUpdater";


    @Override
    public void start() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                new ApiRequester(new ExchangeApiRequest(Currency.BTC.getCode()), new SilentApiHandler() {
                    @Override
                    public void onOkResponse(JSONObject jsonObject) throws JSONException {
                        Currency c = Currency.BTC;
                        HashMap<String, Long> prices = new HashMap<String, Long>();
                        for (Currency curr : Currency.TICKER_CURRENCIES) {
                            if (!curr.equals(c)) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                try {
                                    double price = data.getDouble(curr.getCode()+"x"+c.getCode());
                                    if (c.equals(Currency.BTC) || c.equals(Currency.FAC)) {
                                        prices.put(curr.getCode(), Math.round(price * 1e8d));
                                    } else {
                                        prices.put(curr.getCode(), Math.round(price * 1e2d));
                                    }
                                } catch (Exception ignored) {}

                            }
                        }

                        setPrice(c, prices);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                if (!stopped) {
                    handler.postDelayed(this, 300000); //Each 5 min
                }
            }
        };

        handler.post(r);

    }

    private void setPrice(Currency currency, HashMap<String, Long> prices) {
        switch (currency) {
            case BTC:
                Prices.setBitcoin(prices);
                break;
            case FAC:
                Prices.setFair(prices);
                break;
            case EUR:
                Prices.setEuro(prices);
                break;
            case MXN:
                Prices.setPeso(prices);
                break;
            case PLN:
                Prices.setZloti(prices);
                break;
            case USD:
                Prices.setDollar(prices);
                break;
        }

    }
}