package crea.wallet.lite.ui.adapter;

import android.app.Activity;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Address;
import org.creativecoinj.core.Coin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.util.coin.CoinConverter;
import crea.wallet.lite.wallet.WalletHelper;

/**
 * Created by ander on 17/11/16.
 */
public class SwapAddressAdapter extends RecyclerAdapter<SwapAddressAdapter.ViewHolder, AddressBalanceItem> {

    private static final String TAG = "SwapAddressAdapter";

    private List<AddressBalanceItem> addresses;
    private Activity activity;

    public SwapAddressAdapter(Activity activity, @NonNull List<AddressBalanceItem> addresses) {
        super(activity);
        this.activity = activity;
        this.addresses = addresses;
    }

    private Activity getActivity()  {
        return activity;
    }

    @Override
    protected List<AddressBalanceItem> getItemList() {
        return addresses;
    }

    public void addAll(Collection<AddressBalanceItem> collection) {
        addresses.addAll(collection);
        notifyDataChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.address_book_row, parent, false));
    }

    @Override
    public void onBindHolder(ViewHolder holder, int position, AddressBalanceItem item) {
        holder.address.setText(item.getAddress());
        holder.amountView.setVisibility(View.VISIBLE);

        holder.amountBtc.setText(item.getBalance() + " CREA");
        holder.parentItemRow.setBackgroundColor(item.isSelected() ? activity.getResources().getColor(R.color.colorPrimaryTranslucent) : android.R.attr.selectableItemBackground);
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        TextView address;
        TextView amountBtc;
        View amountView;
        View parentItemRow;

        public ViewHolder(View v) {
            super(v);
            parentItemRow = v.findViewById(R.id.parent_item_row);
            address = (TextView) v.findViewById(R.id.address_btc);
            amountBtc = (TextView) v.findViewById(R.id.amount_btc);
            amountView =  v.findViewById(R.id.balance);
        }
    }
}
