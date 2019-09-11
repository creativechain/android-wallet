package crea.wallet.lite.swap.process;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import crea.wallet.lite.swap.api.ApiError;
import crea.wallet.lite.swap.api.CondenserApi;
import crea.wallet.lite.http.RPCResponse;
import crea.wallet.lite.http.RPCResponseListener;
import crea.wallet.lite.swap.AccountName;
import crea.wallet.lite.swap.RoleKeyGenerator;
import crea.wallet.lite.swap.crypto.ECKey;
import crea.wallet.lite.swap.crypto.PrivateKeyType;
import crea.wallet.lite.swap.crypto.PublicKey;
import crea.wallet.lite.util.task.Task;

public class AccountManager {

    private AccountName accountName;

    public AccountManager(AccountName accountName) {
        this.accountName = accountName;
    }

    public AccountManager(String name) {
        this.accountName = new AccountName(name);
    }

    public void login(@NonNull String password, final Task<ApiError> doneTask) throws JSONException {
        final RoleKeyGenerator keyGenerator = new RoleKeyGenerator(accountName, password);
        keyGenerator.addRoles(PrivateKeyType.OWNER, PrivateKeyType.ACTIVE, PrivateKeyType.MEMO, PrivateKeyType.POSTING);

        CondenserApi condenserApi = new CondenserApi();
        condenserApi.setResponseListener(new RPCResponseListener() {
            @Override
            public void onOkResponse(RPCResponse response) throws Exception {
                JSONArray jsonArray = response.getResultArray();
                JSONObject jsonAccount = jsonArray.getJSONObject(0);

                boolean logged = true;
                Map<PrivateKeyType, ECKey> keys = keyGenerator.generateKeys();
                for (PrivateKeyType t : keys.keySet()) {
                    ECKey k = keys.get(t);
                    PublicKey pub = new PublicKey(k);

                    String typeString = t.toString().toLowerCase();
                    if (t == PrivateKeyType.MEMO) {
                        typeString += "_key";
                        String authKey = jsonAccount.getString(typeString);
                        logged = authKey.equals(pub.getAddressFromPublicKey());
                    } else {
                        String authKey = jsonAccount.getJSONObject(typeString).getJSONArray("key_auths").getJSONArray(0).getString(0);
                        logged = authKey.equals(pub.getAddressFromPublicKey());
                    }

                    if (!logged) {
                        break;
                    }
                }

                if (doneTask != null) {
                    if (logged) {
                        doneTask.doTask(null);
                    } else {
                        doneTask.doTask(ApiError.LOGIN_FAILED);
                    }
                }

            }

            @Override
            public void onErrorResponse(RPCResponse response) throws Exception {
                if (doneTask != null) {
                    doneTask.doTask(ApiError.RPC_RESPONSE_ERROR);
                }
            }
        });
        condenserApi.getAccounts(accountName.getName());
    }
}
