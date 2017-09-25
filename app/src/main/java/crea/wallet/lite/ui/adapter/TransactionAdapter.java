package crea.wallet.lite.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.util.coin.CoinConverter;
import crea.wallet.lite.util.wrapper.TxInfo;

import com.google.common.collect.Lists;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Transaction;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static crea.wallet.lite.wallet.WalletHelper.INSTANCE;

/**
 * Created by ander on 16/11/16.
 */
public class TransactionAdapter extends RecyclerAdapter<TransactionAdapter.ViewHolder, Transaction> {

    private static final String TAG = "TransactionAdapter";

    public interface OnOptionsClickListener {
        void onOptionsClick(View view, ViewHolder holder, TxInfo txInfo);
    }

    private OnOptionsClickListener optionsClickListener;

    public TransactionAdapter(Activity activity) {
        super(activity);
    }

    public void setOptionsClickListener(OnOptionsClickListener optionsClickListener) {
        this.optionsClickListener = optionsClickListener;
    }

    @Override
    protected List<Transaction> getItemList() {
        List<Transaction> transactions = Lists.newArrayList(INSTANCE.getWallet().getTransactions(false));

        Collections.sort(transactions, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                return o1.getUpdateTime().getTime() > o2.getUpdateTime().getTime() ? -1 : 1;
            }
        });
        return transactions;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = getLayoutInflater().inflate(R.layout.transaction_row, parent, false);
        return new ViewHolder(convertView);
    }

    @Override
    public void onBindHolder(final ViewHolder holder, int position, Transaction transaction) {
        final TxInfo txInfo = new TxInfo(transaction);
        if (txInfo.isSentFromUser()) {
            holder.txStatusIcon.setTextColor(activity.getResources().getColor(txInfo.isConfirmed() ? R.color.red : R.color.unconfirmed_output));
        } else {
            holder.txStatusIcon.setTextColor(activity.getResources().getColor(txInfo.isConfirmed() ? R.color.green : R.color.unconfirmed_input));
        }

        org.creativecoinj.core.Coin fee = org.creativecoinj.core.Coin.valueOf(txInfo.getFee().longValue());
        org.creativecoinj.core.Coin coin = txInfo.getTransactionedCoin();
        holder.txDate.setText(DateUtils.getRelativeTimeSpanString(txInfo.getTime(), System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS ));
        holder.destinies.setText(txInfo.isRBFTransaction() ? getString(R.string.raise_fee_tx) : TextUtils.join(", ", txInfo.getAddressesResolved()));
        holder.destinyAmount.setText(coin.toFriendlyString());
        holder.feeAmount.setText(fee.toFriendlyString());

        if (txInfo.isSentFromUser()) {
            Configuration conf = Configuration.getInstance();
            AbstractCoin price = conf.getPriceForMainCurrency();
            String feeConversion = new CoinConverter()
                    .amount(transaction.getFee())
                    .price(price).toString();

            holder.feeAmountFiat.setText(feeConversion);
        } else {
            holder.feeRow.setVisibility(View.GONE);
        }

        holder.optionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optionsClickListener != null) {
                    optionsClickListener.onOptionsClick(v, holder, txInfo);
                }
            }
        });

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        View feeRow;
        TextView txDate;
        TextView txStatusIcon;
        TextView destinies;
        TextView destinyAmount;
        TextView feeAmount;
        TextView feeAmountFiat;
        View optionsView;

        public ViewHolder(View v) {
            super(v);
            feeRow = v.findViewById(R.id.fee_row);
            txDate = (TextView) v.findViewById(R.id.transaction_date);
            txStatusIcon = (TextView) v.findViewById(R.id.transaction_status_icon);
            destinies = (TextView) v.findViewById(R.id.destination_address);
            destinyAmount = (TextView) v.findViewById(R.id.destination_amount);
            feeAmount = (TextView) v.findViewById(R.id.fee_amount_btc);
            feeAmountFiat = (TextView) v.findViewById(R.id.fee_amount_fiat);
            optionsView = v.findViewById(R.id.transaction_options);
        }
    }
}
