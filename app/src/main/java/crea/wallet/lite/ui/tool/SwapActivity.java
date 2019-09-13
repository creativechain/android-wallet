package crea.wallet.lite.ui.tool;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.zanjou.http.debug.Logger;
import com.zanjou.http.request.Request;
import com.zanjou.http.response.JsonResponseListener;

import org.creativecoinj.core.Address;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.ui.adapter.AddressBalanceItem;
import crea.wallet.lite.ui.base.BaseSwapActivity;
import crea.wallet.lite.util.Utils;
import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.util.wrapper.IntentUtils;

import static crea.wallet.lite.wallet.WalletHelper.INSTANCE;

public class SwapActivity extends BaseSwapActivity {

    private List<AddressBalanceItem> addressBalanceItems;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap);

        Bundle extras = getIntent().getExtras();
        addressBalanceItems = extras.getParcelableArrayList("balances");
        username = extras.getString("username");

        if (addressBalanceItems.size() > 0) {
            AlertDialog aDialog = DialogFactory.alert(this, R.string.swap_confirmation, getString(R.string.swap_confirmation_message, username));
            aDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.make_swap), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    IntentUtils.checkPin(SwapActivity.this);
                }
            });

            aDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            });

            aDialog.show();
        } else {
            Toast.makeText(WalletApplication.INSTANCE, R.string.swap_no_addresses, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void prepareSwap(final String pin) {
        final ProgressDialog pDialog = DialogFactory.progress(this, R.string.swap_signatures, R.string.swap_signing);
        pDialog.show();

        final String message = Utils.generateRandomString(16);

        final Address[] addresses = new Address[addressBalanceItems.size()];
        for (int x  =0; x < addressBalanceItems.size(); x++) {
            addresses[x] = addressBalanceItems.get(x).toAddress();
        }

        new AsyncTask<Void, Void, Map<String, String>> (){
            @Override
            protected Map<String, String> doInBackground(Void... voids) {
                return INSTANCE.signMessage(pin, message, addresses);
            }

            @Override
            protected void onPostExecute(Map<String, String> signeds) {
                pDialog.dismiss();
                makeSwap(signeds, message);
            }
        }.execute();
    }

    private void makeSwap(Map<String, String> signatures, String message) {
        final ProgressDialog pDialog = DialogFactory.progress(this, R.string.swap_sending, R.string.swap_sending_message);
        pDialog.show();


        Request.create(Constants.SWAP.PLATFORM_API + "/swap")
                .setLogger(new Logger(Logger.ERROR))
                .setMethod(Request.POST)
                .setTimeout(120)
                .addHeader("Authorization", "Bearer " + Configuration.getInstance().getAccessToken())
                .addParameter("user", username)
                .addParameter("message", message)
                .addParameter("signatures", new JSONObject(signatures).toString())
                .setResponseListener(new JsonResponseListener() {
                    @Override
                    public void onOkResponse(JSONObject jsonObject) throws JSONException {
                        pDialog.dismiss();
                        AlertDialog aDialog = DialogFactory.successAlert(SwapActivity.this, R.string.swap_success, getString(R.string.swap_success_message, username));
                        aDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.finish), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                finish();
                            }
                        });

                        aDialog.show();
                    }

                    @Override
                    public void onErrorResponse(JSONObject jsonObject) throws JSONException {
                        pDialog.dismiss();
                        AlertDialog aDialog = DialogFactory.error(SwapActivity.this, "Error", jsonObject.getString("message"));
                        aDialog.setButton(Dialog.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                finish();
                            }
                        });

                        aDialog.show();
                    }

                    @Override
                    public void onParseError(JSONException e) {
                        pDialog.dismiss();
                        Toast.makeText(SwapActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }).execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                prepareSwap(data.getStringExtra(PinActivity.EXTRA_CODE));
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == IntentUtils.PIN) {
                Toast.makeText(this, R.string.bad_pn_too_many_times, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
