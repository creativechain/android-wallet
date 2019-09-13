package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zanjou.http.debug.Logger;
import com.zanjou.http.request.Request;
import com.zanjou.http.request.RequestListener;
import com.zanjou.http.response.JsonResponseListener;

import org.creativecoinj.core.Address;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.ui.adapter.AddressBalanceItem;
import crea.wallet.lite.ui.adapter.RecyclerAdapter;
import crea.wallet.lite.ui.adapter.SwapAddressAdapter;
import crea.wallet.lite.ui.base.BaseSwapActivity;

import static crea.wallet.lite.wallet.WalletHelper.INSTANCE;

public class SwapBalanceActivity extends BaseSwapActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "SwapBalanceActivity";

    private TextView totalAmount;
    private TextView receiveAmount;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView swapAddressList;

    private ArrayList<AddressBalanceItem> selectedItems = new ArrayList<>();
    private SwapAddressAdapter addressAdapter;

    private String username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_balance);

        Bundle extras = getIntent().getExtras();
        username = extras.getString("username");
        totalAmount = findViewById(R.id.total_amount);
        receiveAmount = findViewById(R.id.receive_amount);
        swapAddressList = findViewById(R.id.address_list);
        refreshLayout = findViewById(R.id.refreshLayout);

        swapAddressList.setLayoutManager(new LinearLayoutManager(this));
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark);

        addressAdapter = new SwapAddressAdapter(this, new ArrayList<AddressBalanceItem>());
        addressAdapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener<AddressBalanceItem>() {
            @Override
            public void OnItemClick(View v, int position, AddressBalanceItem addressBalanceItem) {
                if (selectedItems.contains(addressBalanceItem)) {
                    selectedItems.remove(addressBalanceItem);
                    addressBalanceItem.setSelected(false);
                } else {
                    selectedItems.add(addressBalanceItem);
                    addressBalanceItem.setSelected(true);
                }

                addressAdapter.notifyItemChanged(position);
            }

            @Override
            public boolean OnItemLongClick(View v, int position, AddressBalanceItem addressBalanceItem) {
                return false;
            }
        });


        swapAddressList.setAdapter(addressAdapter);

        View makeSwapButton = findViewById(R.id.make_swap);
        makeSwapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent swapIntent = new Intent(SwapBalanceActivity.this, SwapActivity.class);
                swapIntent.putExtra("balances", selectedItems);
                swapIntent.putExtra("username", username);
                startActivity(swapIntent);
            }
        });
        fetchAddresses();

    }

    @Override
    public void onRefresh() {
        fetchAddresses();
    }

    private void fetchAddresses() {
        List<Address> swapAddresses = INSTANCE.getAllAddressesForSwap();
        List<String> requestAddresses = new ArrayList<>(swapAddresses.size());
        for (Address a : swapAddresses) {
            requestAddresses.add(a.toBase58());
        }

        String addresses = TextUtils.join(",", requestAddresses);

        Request.create(Constants.SWAP.PLATFORM_API + "/getBalances")
                .setLogger(new Logger(Logger.ERROR))
                .setMethod(Request.POST)
                .addParameter("addresses", addresses)
                .addHeader("Authorization", "Bearer " + Configuration.getInstance().getAccessToken())
                .setRequestListener(new RequestListener() {
                    @Override
                    public void onStart() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshLayout.setRefreshing(true);
                            }
                        });

                    }

                    @Override
                    public void onFinish() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshLayout.setRefreshing(false);
                            }
                        });

                    }

                    @Override
                    public void onUploadProgress(float progress) {

                    }

                    @Override
                    public void onConnectionError(Exception e) {

                    }
                })
                .setResponseListener(new JsonResponseListener() {
                    @Override
                    public void onOkResponse(JSONObject jsonObject) throws JSONException {

                        jsonObject = jsonObject.getJSONObject("data");
                        final double swapTotalAmount = jsonObject.getDouble("total_amount");
                        final double swapReceiveAmount = jsonObject.getDouble("receive_amount");

                        JSONObject jBalances = jsonObject.getJSONObject("balances");
                        final List<AddressBalanceItem> swapBalances = new ArrayList<>();
                        Iterator<String> it = jBalances.keys();
                        while(it.hasNext()) {
                            String address = it.next();
                            swapBalances.add(new AddressBalanceItem(address, jBalances.getDouble(address)));
                        }

                        Log.d(TAG, "total_balance: " + swapTotalAmount + ", receive_amount: " + swapReceiveAmount + ", addresses: " + swapBalances.size() );
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                totalAmount.setText(getString(R.string.swap_total_amount, String.valueOf(swapTotalAmount)));
                                receiveAmount.setText(getString(R.string.swap_to_receive_amount, String.valueOf(swapReceiveAmount)));
                                addressAdapter.addAll(swapBalances);
                                swapAddressList.invalidate();
                            }
                        });

                    }

                    @Override
                    public void onErrorResponse(JSONObject jsonObject) throws JSONException {
                        Log.e(TAG, "ResponseError: " + jsonObject.toString());
                    }

                    @Override
                    public void onParseError(JSONException e) {
                        e.printStackTrace();
                    }
                }).execute();

    }
}
