package crea.wallet.lite.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;

public class RPCResponse {

    private int status;
    private String response;

    public RPCResponse(int status, String response) {
        this.status = status;
        this.response = response;
    }

    public int getStatus() {
        return status;
    }

    public boolean hasStatusError() {
        return status >= 400;
    }

    public boolean hasRPCError() throws JSONException {
        return getResponseAsJSON().has("error");
    }

    public String getResponse() {
        return response;
    }

    public JSONObject getResultObject() throws JSONException {
        return getResponseAsJSON().getJSONObject("result");
    }

    public JSONArray getResultArray() throws JSONException {
        return getResponseAsJSON().getJSONArray("result");
    }

    public JSONObject getResponseAsJSON() throws JSONException {
        return new JSONObject(this.response);
    }

    @Override
    public String toString() {
        return "RPCResponse: " + status + " - [ " + this.response + "]";
    }

}