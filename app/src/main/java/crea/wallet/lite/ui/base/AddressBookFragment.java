package crea.wallet.lite.ui.base;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.creativecoinj.core.Address;

import crea.wallet.lite.db.BookAddress;
import crea.wallet.lite.R;
import crea.wallet.lite.ui.adapter.BookAddressAdapter;
import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.util.wrapper.FormUtils;
import crea.wallet.lite.util.wrapper.QR;

import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;

/**
 * Created by ander on 17/11/16.
 */
public abstract class AddressBookFragment extends FragmentContext {

    private static final String TAG = "AddressBookFragment";

    protected RecyclerView addressBookList;
    protected FloatingActionButton addButton;
    protected BookAddressAdapter adapter;
    protected View noAddress;

    @Override
    public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layout = getLayout();
        return inflater.inflate(layout, null);
    }

    @LayoutRes
    protected abstract int getLayout();

    protected abstract void onBookAddressClick(View v, BookAddress address);
    protected abstract void onBookAddressLongClick(View v, BookAddress address);

    protected void showEditDialog(final BookAddress address, final boolean canEditAddress) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.address_edit_dialog, null);
        final EditText editAddress = (EditText) v.findViewById(R.id.address);
        final EditText editLabel = (EditText) v.findViewById(R.id.label);

        editLabel.setText(address.getLabel());
        editAddress.setEnabled(canEditAddress);
        if (address.getAddress() != null) {
            editAddress.setText(address.getAddress());
        }

        final AlertDialog aDialog = DialogFactory.alert(getActivity(), R.string.edit_address, v);
        aDialog.setCancelable(false);
        aDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), (DialogInterface.OnClickListener)null);
        aDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (DialogInterface.OnClickListener)null);
        aDialog.show();

        Button positive = aDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = aDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick, Button positive");
                boolean error = false;

                if (FormUtils.isEmpty(editAddress)) {
                    editAddress.setError(getString(R.string.empty_error_field));
                    error = true;
                } else {
                    try {
                        Address.fromBase58(NETWORK_PARAMETERS, editAddress.getText().toString());
                    } catch (Exception e) {
                        error = true;
                        editAddress.setError(getString(R.string.invalid_bitcoin_address));
                    }
                }

                Log.d(TAG, "hasError: " + error);
                if (!error) {
                    String addressString = editAddress.getText().toString().trim();
                    String label = editLabel.getText().toString().trim();
                    address.setAddress(addressString)
                            .setLabel(label)
                            .save();
                    aDialog.dismiss();
                    notifyDataChanged();
                }
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aDialog.dismiss();
            }
        });
    }

    protected void notifyDataChanged() {
        adapter.notifyDataChanged();
        invalidateAdapter();
    }

    protected void invalidateAdapter() {
        boolean hasTransactions = !adapter.isEmpty();

        addressBookList.setVisibility(hasTransactions ? View.VISIBLE : View.GONE);
        noAddress.setVisibility(hasTransactions ? View.GONE : View.VISIBLE);
    }

    protected void showQR(BookAddress address) {
        QR.getAddressQrDialog(getActivity(), address.getAddress()).show();
    }

    public abstract CharSequence getTitle();
}
