package crea.wallet.lite.http;

import com.zanjou.http.debug.Logger;
import com.zanjou.http.request.Request;
import com.zanjou.http.response.ResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import crea.wallet.lite.util.Utils;

public class RPCClient {

    private static final String RPC_VERSION = "2.0";

    private String url;
    private ResponseListener responseListener;

    public RPCClient(String url) {
        this.url = url;
    }

    public void setResponseListener(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    private JSONObject getJsonRPC() throws JSONException {
        JSONObject rpc = new JSONObject();
        rpc.put("jsonrpc", RPC_VERSION);
        rpc.put("id", Utils.getRandom(0, Integer.MAX_VALUE-1));
        return rpc;
    }

    private void exec(JSONObject data) {
        exec(data.toString());
    }

    private void exec(String data) {
        Request.create(this.url)
                .setLogger(new Logger(Logger.DEBUG))
                .setTimeout(120)
                .postBody("application/json", data)
                .setResponseListener(responseListener)
                .execute();
    }

    protected void call(String method, JSONArray params) throws JSONException {
        JSONObject rpc = getJsonRPC();
        rpc.put("method", method);
        rpc.put("params", params);
        exec(rpc);
    }

    protected void call(String method, JSONObject params) throws JSONException {
        JSONObject rpc = getJsonRPC();
        rpc.put("method", method);
        rpc.put("params", params);
        exec(rpc);
    }
}
