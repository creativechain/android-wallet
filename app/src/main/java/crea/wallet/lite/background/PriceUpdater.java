package crea.wallet.lite.background;

import android.os.AsyncTask;
import android.util.Log;

import crea.wallet.lite.application.Configuration;

import com.chip_chap.services.asynchttp.net.ApiRequester;
import com.chip_chap.services.asynchttp.net.handler.SilentApiHandler;
import com.chip_chap.services.asynchttp.net.util.ApiRequest;
import com.chip_chap.services.calls.base.BaseGetApiRequest;
import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.coin.base.Coin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ander on 16/02/16.
 */
public class PriceUpdater extends Updater {

    private static final String TAG = "PriceUpdater";

    private static Currency[] TICKER_CURRENCIES = {Currency.EUR, Currency.GBP, Currency.MXN, Currency.BTC, Currency.USD};

    @Override
    public void start() {
        Runnable r = new Runnable() {
            @Override
            public void run() {

                for (final Currency c : TICKER_CURRENCIES) {

                    new ApiRequester(new TickerRequest(c), new SilentApiHandler() {
                        @Override
                        public void onOkResponse(JSONObject jsonObject) throws JSONException {
                            jsonObject = jsonObject.getJSONArray("data").getJSONObject(0);

                            Coin price = Coin.fromCurrency(c, jsonObject.getDouble("price_" + c.getCode().toLowerCase()));

                            Configuration conf = Configuration.getInstance();
                            conf.setCreaPrice(price.getCurrency(), price.getLongValue());
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                }


                if (!stopped) {
                    handler.postDelayed(this, 300000); //Each 5 min
                }
            }
        };

        handler.post(r);

    }

    private static class TickerRequest extends BaseGetApiRequest {

        private Currency currency;

        public TickerRequest(Currency currency) {
            this.currency = currency;
        }

        @Override
        public String getURL() {
            return "https://api.coinmarketcap.com/v1/ticker/creativecoin/";
        }

        @Override
        public Map<String, String> getURLParams() {
            Map<String, String> params = new HashMap<>();
            params.put("convert", currency.getCode());

            return params;
        }

    }
}