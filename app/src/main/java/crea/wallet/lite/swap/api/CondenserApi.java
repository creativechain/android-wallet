package crea.wallet.lite.swap.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CondenserApi extends Api {

    public static final JSONArray DEFAULT_PARAMS = new JSONArray();

    public CondenserApi() {
        super("condenser");
    }


    protected void call(String method) throws JSONException {
        JSONArray data = new JSONArray();
        super.call(method, data);
    }

    @Override
    protected void call(String method, JSONArray params) throws JSONException {
        JSONArray data = new JSONArray();
        data.put(params);
        super.call(method, data);
    }

    @Override
    protected void call(String method, JSONObject params) throws JSONException {
        JSONArray data = new JSONArray();
        data.put(params);
        super.call(method, data);
    }

    public void getAccounts(String... accounts) throws JSONException {
        JSONArray params = new JSONArray();

        for (String a : accounts) {
            params.put(a);
        }

        call("get_accounts", params);
    }

    public void getDynamicGlobalProperties() throws JSONException {
        call("get_dynamic_global_properties");
    }
}