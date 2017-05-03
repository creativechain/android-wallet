package crea.wallet.lite.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.util.CoinConverter;
import crea.wallet.lite.util.TimeUtils;
import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.Method;
import com.chip_chap.services.cash.coin.BitCoin;
import com.chip_chap.services.cash.coin.base.Coin;
import com.chip_chap.services.transaction.Btc2BtcTransaction;
import com.chip_chap.services.transaction.ChipChapTransaction;

import java.util.List;

/**
 * Created by ander on 16/11/16.
 */
public class TransactionAdapter extends RecyclerAdapter<TransactionAdapter.ViewHolder, Btc2BtcTransaction> {

    private static final String TAG = "TransactionAdapter";

    public TransactionAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected List<Btc2BtcTransaction> getItemList() {
        return ChipChapTransaction.find(Btc2BtcTransaction.class);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = getLayoutInflater().inflate(R.layout.transaction_row, parent, false);
        return new ViewHolder(convertView);
    }

    @Override
    public void onBindHolder(ViewHolder holder, int position, Btc2BtcTransaction transaction) {
        if (transaction.getExchangeMethod() == Method.B2BI) {
            holder.txStatusIcon.setTextColor(activity.getResources().getColor(transaction.isConfirmed() ? R.color.green : R.color.unconfirmed_input));
        } else {
            holder.txStatusIcon.setTextColor(activity.getResources().getColor(transaction.isConfirmed() ? R.color.red : R.color.unconfirmed_output));
        }

        org.creativecoinj.core.Coin fee = org.creativecoinj.core.Coin.valueOf(transaction.getFee());
        org.creativecoinj.core.Coin coin = org.creativecoinj.core.Coin.valueOf(transaction.amountToCoin().getLongValue());
        holder.txDate.setText(TimeUtils.getTimeString(transaction.getDateInLong()));
        holder.destinies.setText(transaction.getBeneficiary());
        holder.destinyAmount.setText(coin.toFriendlyString());
        holder.feeAmountBtc.setText(fee.toFriendlyString());

        Coin feeConversion = new CoinConverter()
                .amount(BitCoin.valueOf(transaction.getFee()))
                .price(Configuration.getInstance().getBtcPrice(Currency.EUR)).getConversion();



        holder.feeAmountFiat.setText(feeConversion.toFriendlyString());
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        TextView txDate;
        TextView txStatusIcon;
        TextView destinies;
        TextView destinyAmount;
        TextView feeAmountBtc;
        TextView feeAmountFiat;

        public ViewHolder(View v) {
            super(v);
            txDate = (TextView) v.findViewById(R.id.transaction_date);
            txStatusIcon = (TextView) v.findViewById(R.id.transaction_status_icon);
            destinies = (TextView) v.findViewById(R.id.destination_address);
            destinyAmount = (TextView) v.findViewById(R.id.destination_amount);
            feeAmountBtc = (TextView) v.findViewById(R.id.fee_amount_btc);
            feeAmountFiat = (TextView) v.findViewById(R.id.fee_amount_fiat);
        }
    }
}
