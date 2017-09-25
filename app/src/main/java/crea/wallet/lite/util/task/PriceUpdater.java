package crea.wallet.lite.util.task;

import android.content.Intent;

import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.util.coin.CoinUtils;

import com.zanjou.http.debug.Logger;
import com.zanjou.http.request.Request;
import com.zanjou.http.response.OkJsonResponseListener;

import org.creativecoinj.core.AbstractCoin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import static crea.wallet.lite.application.WalletApplication.INSTANCE;

/**
 * Created by ander on 16/02/16.
 */
public class PriceUpdater extends Updater {

    private static final String TAG = "PriceUpdater";

    private static String[] TICKER_CURRENCIES = {"EUR", "GBP","MXN", "BTC", "USD"};

    @Override
    public void start() {
        Runnable r = new Runnable() {
            @Override
            public void run() {

                for (final String c : TICKER_CURRENCIES) {
                    Request.create("https://api.coinmarketcap.com/v1/ticker/creativecoin/")
                            .setMethod(Request.GET)
                            .addParameter("convert", c)
                            .setLogger(new Logger(Logger.ERROR))
                            .setResponseListener(new OkJsonResponseListener() {

                                @Override
                                public void onOkResponse(String content) {
                                    try {
                                        JSONArray jarray = new JSONArray(content);
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("data", jarray);
                                        onOkResponse(jsonObject);
                                    } catch (JSONException e) {
                                        onParseError(e);
                                    }

                                }

                                @Override
                                public void onOkResponse(JSONObject jsonObject) throws JSONException {
                                    jsonObject = jsonObject.getJSONArray("data").getJSONObject(0);

                                    AbstractCoin price = CoinUtils.valueOf(c, jsonObject.getDouble("price_" + c.toLowerCase()));

                                    Configuration conf = Configuration.getInstance();
                                    conf.setCreaPrice(price.getCurrencyCode(), price.getValue());
                                }
                            }).execute();
                }

                INSTANCE.sendBroadcast(new Intent(BlockchainBroadcastReceiver.PRICE_UPDATE));

                if (!stopped) {
                    Configuration conf = Configuration.getInstance();
                    handler.postDelayed(this, conf.getPriceUpdateInterval());
                }
            }
        };

        handler.post(r);

    }
}