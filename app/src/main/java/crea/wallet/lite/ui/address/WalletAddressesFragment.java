package crea.wallet.lite.ui.address;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.creativecoinj.core.Address;

import crea.wallet.lite.util.task.WalletExporter;
import crea.wallet.lite.db.BookAddress;
import crea.wallet.lite.R;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.ui.adapter.BookAddressAdapter;
import crea.wallet.lite.ui.adapter.RecyclerAdapter;
import crea.wallet.lite.ui.base.AddressBookFragment;
import crea.wallet.lite.ui.tool.PinActivity;
import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.util.wrapper.IntentUtils;
import crea.wallet.lite.util.wrapper.QR;
import crea.wallet.lite.util.task.Task;
import crea.wallet.lite.wallet.WalletHelper;

import static android.app.Activity.RESULT_OK;

/**
 * Created by ander on 17/11/16.
 */
public class WalletAddressesFragment extends AddressBookFragment {

    private static final String TAG = "WalletAddressesFragment";

    private Address exportAddress;
    @Override
    protected int getLayout() {
        return R.layout.fragment_wallet_address;
    }

    @Override
    protected void onBookAddressClick(View v, final BookAddress address) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.book_wallet_address_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                switch (id) {
                    case R.id.action_copy:
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Creativecoin address", address.getAddress());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getActivity(), getString(R.string.address_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.action_edit:
                        showEditDialog(address, false);
                        break;
                    case R.id.action_export:
                        exportAddress = address.toAddress();
                        IntentUtils.checkPin(WalletAddressesFragment.this);
                        break;
                    case R.id.action_show_qr:
                        showQR(address);
                        break;

                }
                return false;
            }
        });

        popupMenu.show();
    }

    @Override
    protected void onBookAddressLongClick(View v, BookAddress address) {

    }

    @Override
    public CharSequence getTitle() {
        return WalletApplication.INSTANCE.getString(R.string.wallet_addresses);
    }

    @Override
    public void afterInitialize(View view, Bundle savedInstanceState) {
        addressBookList = findViewById(R.id.wallet_list);
        addButton = findViewById(R.id.wallet_button);
        noAddress = findViewById(R.id.no_wallet_address);

        addressBookList.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new BookAddressAdapter(getActivity(), true);
        addressBookList.setAdapter(adapter);
        notifyDataChanged();

        adapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener<BookAddress>() {
            @Override
            public void OnItemClick(View v, int position, BookAddress address) {
                BookAddress item = adapter.getItem(position);
                onBookAddressClick(v, item);
            }

            @Override
            public boolean OnItemLongClick(View v, int position, BookAddress address) {
                return false;
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditDialog(new BookAddress().setAddress(getNextAddress().toString()).setMine(true), false);
            }
        });
    }

    private Address getNextAddress() {
        Address a = WalletHelper.INSTANCE.currentMainReceiveAddress();
        BookAddress ba = BookAddress.resolveAddress(a);

        if (ba == null) {
            Log.d(TAG, "Address " + a.toString() + " not found.");
            return a;
        } else {
            Log.d(TAG, "Address " + a.toString() + " found. Generating new...");
            a = WalletHelper.INSTANCE.getNewAddress();
        }

        Log.d(TAG, "Returning " + a.toString());
        return a;
    }

    private void exportPrivKey(String pin) {
        final ProgressDialog pDialog = DialogFactory.progress(getActivity(), R.string.private_key, R.string.getting_private_key);
        pDialog.setCancelable(false);
        pDialog.show();
        new WalletExporter(pin, exportAddress, new Task<Bundle>() {
            @Override
            public void doTask(Bundle data) {
                pDialog.dismiss();
                if (data != null && !data.isEmpty()) {
                    final String seed = data.getString("exported_key");
                    showPrivKey(seed);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.cannot_export_seed), Toast.LENGTH_LONG).show();
                }
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showPrivKey(final String privKey) {
        QR.getPrivKeyQrDialog(getActivity(), privKey).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                String pin = data.getStringExtra(PinActivity.EXTRA_CODE);
                exportPrivKey(pin);
            }
        }
    }
}
