package crea.wallet.lite.ui.adapter;

import android.app.Activity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import crea.wallet.lite.db.BookAddress;
import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.util.coin.CoinConverter;
import crea.wallet.lite.wallet.WalletHelper;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Coin;

import java.util.List;

/**
 * Created by ander on 17/11/16.
 */
public class BookAddressAdapter extends RecyclerAdapter<BookAddressAdapter.ViewHolder, BookAddress> {

    private static final String TAG = "BookAddressAdapter";

    private boolean walletAddresses;

    private Configuration conf = Configuration.getInstance();

    public BookAddressAdapter(Activity activity, boolean walletAddresses) {
        super(activity);
        this.walletAddresses = walletAddresses;
    }

    @Override
    protected List<BookAddress> getItemList() {
        return BookAddress.find(walletAddresses);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.address_book_row, parent, false));
    }

    @Override
    public void onBindHolder(ViewHolder holder, int position, BookAddress item) {
        holder.label.setText(item.getLabel());
        holder.address.setText(item.getAddress());
        holder.amountView.setVisibility(walletAddresses ? View.VISIBLE : View.GONE);
        if (walletAddresses) {
            Coin balance = WalletHelper.INSTANCE.getAddressBalance(item.getAddress());
            AbstractCoin fiat = new CoinConverter()
                            .amount(balance)
                            .price(conf.getCreaPrice(conf.getMainCurrency()))
                            .getConversion();
            holder.amountBtc.setText(balance.toFriendlyString().toLowerCase());
            holder.amountFiat.setText(fiat.toFriendlyString());
        }
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        TextView address;
        TextView amountBtc;
        TextView amountFiat;
        View amountView;

        public ViewHolder(View v) {
            super(v);
            label = (TextView) v.findViewById(R.id.label);
            address = (TextView) v.findViewById(R.id.address_btc);
            amountBtc = (TextView) v.findViewById(R.id.amount_btc);
            amountFiat = (TextView) v.findViewById(R.id.amount_fiat);
            amountView =  v.findViewById(R.id.balance);
        }
    }
}
