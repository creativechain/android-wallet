package crea.wallet.lite.ui.main;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import crea.wallet.lite.application.Constants;
import crea.wallet.lite.db.BookAddress;
import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.service.CreativeCoinService;
import crea.wallet.lite.ui.adapter.RecyclerAdapter;
import crea.wallet.lite.ui.address.AddressBookActivity;
import crea.wallet.lite.ui.tool.CheckAddressActivity;
import crea.wallet.lite.ui.tool.PeerListActivity;
import crea.wallet.lite.ui.tool.SendCoinActivity;
import crea.wallet.lite.ui.adapter.TransactionAdapter;
import crea.wallet.lite.util.coin.CoinConverter;
import crea.wallet.lite.util.wrapper.IntentUtils;
import crea.wallet.lite.util.wrapper.QR;
import crea.wallet.lite.util.wrapper.TxInfo;
import crea.wallet.lite.wallet.WalletHelper;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Address;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.wallet.Wallet;

import static crea.wallet.lite.application.WalletApplication.INSTANCE;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.ACTION_SYNC_STARTED;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.BLOCKCHAIN_RESET;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.LAST_BLOCK_RECEIVED;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.PRICE_UPDATE;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.TRANSACTION_RECEIVED;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.TRANSACTION_SENT;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, TransactionAdapter.OnOptionsClickListener {

    private static final String TAG = "MainActivity";

    private final BlockchainBroadcastReceiver TRANSACTION_RECEIVER = new BlockchainBroadcastReceiver() {
        @Override
        public void onPriceUpdated() {
            invalidateData();
        }

        @Override
        public void onTransactionSend(Transaction transaction) {
            invalidateData();
        }

        @Override
        public void onTransactionReceived(Transaction transaction) {
            invalidateData();
        }

        @Override
        public void onLastDownloadedBlock() {
            invalidateData();
        }

        @Override
        public void onBlockChainReset() {
            invalidateData();
        }

        @Override
        public void onSyncStarted() {
            refreshLayout.setRefreshing(false);
        }
    };

    private TextView totalBtcView;
    private TextView totalFiatView;
    private TextView pendingBtcView;
    private TextView pendingFiatView;
    private TextView valueInFiatView;
    private View pendingLayout;
    private RecyclerView txList;
    private View noTransactions;
    private TransactionAdapter adapter;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalBtcView = (TextView) findViewById(R.id.total_btc);
        totalFiatView = (TextView) findViewById(R.id.total_fiat);
        pendingBtcView = (TextView) findViewById(R.id.pending_btc);
        pendingFiatView = (TextView) findViewById(R.id.pending_fiat);
        valueInFiatView = (TextView) findViewById(R.id.value_in_fiat);
        pendingLayout =  findViewById(R.id.pending_layout);
        noTransactions = findViewById(R.id.no_transactions);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);

        WalletHelper.INSTANCE.addIssuedAddressesToWatch();

        refreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent, R.color.red);
        refreshLayout.setOnRefreshListener(this);

        txList = (RecyclerView) findViewById(R.id.transaction_list);
        txList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this);
        txList.setAdapter(adapter);

        adapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener<Transaction>() {
            @Override
            public void OnItemClick(View v, int position, Transaction tx) {
                Intent txIntent = new Intent(MainActivity.this, CoinTransactionActivity.class);
                txIntent.putExtra("id", tx.getHashAsString());
                startActivity(txIntent);
            }

            @Override
            public boolean OnItemLongClick(View v, int position, Transaction btc2BtcTransaction) {
                return false;
            }
        });

        adapter.setOptionsClickListener(this);
    }

    private void invalidateData() {
        final Configuration conf = Configuration.getInstance();
        final Coin total = WalletHelper.INSTANCE.getBalance(Wallet.BalanceType.ESTIMATED);
        final Coin pending = total.minus(WalletHelper.INSTANCE.getBalance());

        AbstractCoin price = conf.getPriceForMainCurrency();

        if (conf.isExchangeValueEnabled()) {
            valueInFiatView.setVisibility(View.VISIBLE);
            valueInFiatView.setText(getString(R.string.price, price.toFriendlyString()));
        } else {
            valueInFiatView.setVisibility(View.GONE);
        }

        final String totalFiat = new CoinConverter()
                .amount(total)
                .price(price)
                .toString();

        totalBtcView.post(new Runnable() {
            @Override
            public void run() {
                totalBtcView.setText(total.toFriendlyString());
            }
        });

        totalFiatView.post(new Runnable() {
            @Override
            public void run() {
                totalFiatView.setText(totalFiat);
            }
        });

        if (pending.getValue() > 0) {
            pendingLayout.setVisibility(View.VISIBLE);
            pendingBtcView.post(new Runnable() {
                @Override
                public void run() {
                    pendingBtcView.setText(pending.toFriendlyString());
                    pendingBtcView.setVisibility(View.VISIBLE);
                }
            });

            final String pendingFiat = new CoinConverter()
                    .amount(pending)
                    .price(price)
                    .toString();

            pendingFiatView.post(new Runnable() {
                @Override
                public void run() {
                    pendingFiatView.setText(pendingFiat);
                    pendingFiatView.setVisibility(View.VISIBLE);
                }
            });
        } else {
            pendingLayout.setVisibility(View.GONE);
        }


        adapter.notifyDataChanged();
        invalidateAdapter();
        invalidateAddressBook();

    }

    private void invalidateAdapter() {
        boolean hasTransactions = !adapter.isEmpty();

        txList.setVisibility(hasTransactions ? View.VISIBLE : View.GONE);
        noTransactions.setVisibility(hasTransactions ? View.GONE : View.VISIBLE);
    }

    private void invalidateAddressBook() {
        BookAddress.add(WalletHelper.INSTANCE.getReceiveAddresses(), true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cash_in:
                Address address = WalletHelper.INSTANCE.currentMainReceiveAddress();
                showQRCode(address);
                break;
            case R.id.action_cash_out:
                Intent cashOut = new Intent(this, SendCoinActivity.class);
                startActivity(cashOut);
                break;
            case R.id.action_address_book:
                Intent bookIntent = new Intent(this, AddressBookActivity.class);
                startActivity(bookIntent);
                break;
            case R.id.action_check_address:
                Intent checkIntent = new Intent(this, CheckAddressActivity.class);
                startActivity(checkIntent);
                break;
            case R.id.action_settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;
            case R.id.action_network:
                Intent network = new Intent(this, PeerListActivity.class);
                startActivity(network);
        }
        return super.onOptionsItemSelected(item);
    }

    private void showQRCode(final Address ea) {
        Log.e(TAG, "Address: " + ea.toString());
        AlertDialog a = QR.getAddressQrDialog(this, ea);
        a.setCancelable(true);
        a.show();
    }

    @Override
    public void onRefresh() {
        refreshLayout.setRefreshing(true);
        INSTANCE.startBlockchainService(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter iFilter = new IntentFilter(TRANSACTION_RECEIVED);
        iFilter.addAction(PRICE_UPDATE);
        iFilter.addAction(TRANSACTION_SENT);
        iFilter.addAction(LAST_BLOCK_RECEIVED);
        iFilter.addAction(BLOCKCHAIN_RESET);
        iFilter.addAction(ACTION_SYNC_STARTED);
        registerReceiver(TRANSACTION_RECEIVER, iFilter);
        CreativeCoinService.progressBar = valueInFiatView;
        invalidateData();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(TRANSACTION_RECEIVER);
        CreativeCoinService.progressBar = null;
        super.onPause();
    }

    @Override
    public void onOptionsClick(View view, TransactionAdapter.ViewHolder holder, final TxInfo txInfo) {
        PopupMenu optionsMenu = new PopupMenu(this, view);

        optionsMenu.inflate(txInfo.isReplaceable() ? R.menu.replaceable_transaction_options : R.menu.unreplaceable_transaction_options);

        optionsMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                switch (id) {
                    case R.id.action_explore:
                        final String url = Constants.URLS.BLOCKEXPLORER_URL;
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url  + txInfo.getHashAsString()));
                        startActivity(browserIntent);
                        break;
                    case R.id.action_show_qr:
                        QR.getTransactionQrDialog(MainActivity.this, txInfo).show();
                        break;
                    case R.id.action_change_fee:
                        handleChangeFeeAction(txInfo);
                        break;
                }

                return true;
            }
        });

        optionsMenu.show();
    }

    private void handleChangeFeeAction(TxInfo txInfo) {
        IntentUtils.raiseFee(this, txInfo.getTx());
    }
}
