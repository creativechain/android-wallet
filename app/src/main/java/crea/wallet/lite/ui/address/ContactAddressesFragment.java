package crea.wallet.lite.ui.address;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import crea.wallet.lite.application.Constants;
import crea.wallet.lite.db.BookAddress;
import crea.wallet.lite.R;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.ui.adapter.RecyclerAdapter;
import crea.wallet.lite.ui.tool.SendBitcoinActivity;
import crea.wallet.lite.ui.adapter.BookAddressAdapter;
import crea.wallet.lite.ui.base.AddressBookFragment;
import crea.wallet.lite.util.IntentUtils;

import com.chip_chap.services.util.Tags;

import org.creativecoinj.core.Address;
import org.creativecoinj.uri.BitcoinURI;
import org.creativecoinj.uri.BitcoinURIParseException;

import static android.app.Activity.RESULT_OK;

/**
 * Created by ander on 17/11/16.
 */
public class ContactAddressesFragment extends AddressBookFragment {

    private static final String TAG = "WalletAddressesFragment";

    @Override
    public void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_contact_address;
    }

    @Override
    protected void onBookAddressClick(View v, final BookAddress address) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.book_contact_address_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                switch (id) {
                    case R.id.action_copy:
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Bitcoin address", address.getAddress());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getActivity(), getString(R.string.address_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.action_edit:
                        showEditDialog(address, true);
                        break;
                    case R.id.action_show_qr:
                        showQR(address);
                        break;
                    case R.id.action_send:
                        Intent sendIntent = new Intent(getActivity(), SendBitcoinActivity.class);
                        sendIntent.putExtra(Tags.ADDRESS, address.getAddress());
                        startActivity(sendIntent);
                        break;
                    case R.id.action_delete:
                        address.delete();
                        notifyDataChanged();
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
        return WalletApplication.INSTANCE.getString(R.string.contacts_addresses);
    }

    @Override
    public void afterInitialize(View view, Bundle savedInstanceState) {
        addressBookList = findViewById(R.id.contact_list);
        addButton = findViewById(R.id.contact_button);
        noAddress = findViewById(R.id.no_contact_address);

        addressBookList.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new BookAddressAdapter(getActivity(), false);
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
                showEditDialog(new BookAddress(), true);
            }
        });
    }

    private void showEditDialog(Address address) {
        BookAddress bookAddress = BookAddress.resolveAddress(address);

        if (bookAddress == null) {
            bookAddress = new BookAddress();
            bookAddress.setAddress(address.toBase58());
        }

        showEditDialog(bookAddress, true);

    }

    private void handleBitcoinUri(Uri uri) {
        Address address = null;
        try {
            BitcoinURI btcUri = new BitcoinURI(uri.toString());
            address = btcUri.getAddress();
        } catch (BitcoinURIParseException e) {
            try {
                String data = uri.toString();
                address = Address.fromBase58(Constants.WALLET.NETWORK_PARAMETERS, data);
            } catch (Exception e1) {
                Toast.makeText(getActivity(), R.string.not_found_valid_data, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        if (address != null) {
            showEditDialog(address);
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_btc_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_scan:
                IntentUtils.startQRScanner(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.QR_SCAN) {
                handleBitcoinUri(data.getData());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
