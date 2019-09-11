package crea.wallet.lite.swap.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import crea.wallet.lite.application.Constants;
import crea.wallet.lite.http.RPCClient;

public class Api extends RPCClient {

    private static final String URL = Constants.SWAP.CREA_NODES_URL;
    private String api;

    protected Api(String api) {
        super(URL);
        this.api = api;
    }

    @Override
    protected void call(String method, JSONArray params) throws JSONException {
        super.call(api + "_api." + method, params);
    }

    @Override
    protected void call(String method, JSONObject params) throws JSONException {
        super.call(api + "_api." + method, params);
    }
}