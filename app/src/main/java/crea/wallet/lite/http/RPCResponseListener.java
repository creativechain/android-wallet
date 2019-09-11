package crea.wallet.lite.http;

import com.zanjou.http.response.ResponseData;
import com.zanjou.http.response.ResponseListener;

public abstract class RPCResponseListener implements ResponseListener {

    @Override
    public void onResponse(ResponseData responseData) {
        RPCResponse response = new RPCResponse(responseData.getResponseCode(), responseData.getDataAsString());

        try {
            if (response.hasStatusError() || response.hasRPCError()) {
                onErrorResponse(response);
            } else {
                onOkResponse(response);
            }
        } catch (Exception e) {
            onError(e);
        }

    }

    public abstract void onOkResponse(RPCResponse response) throws Exception;
    public abstract void onErrorResponse(RPCResponse response) throws Exception;

    public void onError(Exception e) {
        e.printStackTrace();
    }
}
