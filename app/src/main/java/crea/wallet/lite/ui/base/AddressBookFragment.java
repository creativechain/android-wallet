package crea.wallet.lite.ui.base;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import crea.wallet.lite.db.BookAddress;
import crea.wallet.lite.R;
import crea.wallet.lite.ui.adapter.BookAddressAdapter;
import crea.wallet.lite.util.DialogFactory;
import crea.wallet.lite.util.FormUtils;
import crea.wallet.lite.util.QR;

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

        if (!canEditAddress) {
            editAddress.setEnabled(false);
        }

        AlertDialog aDialog = DialogFactory.alert(getActivity(), R.string.edit_address, v);
        aDialog.setCancelable(false);
        aDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                boolean error = false;

                if (FormUtils.isEmpty(editAddress)) {
                    editAddress.setError(getString(R.string.empty_error_field));
                    error = true;
                }

                if (!error) {
                    String addressString = editAddress.getText().toString().trim();
                    String label = editLabel.getText().toString().trim();
                    address.setAddress(addressString)
                            .setLabel(label)
                            .save();
                    dialogInterface.dismiss();
                    adapter.notifyDataChanged();
                }

            }
        });

        aDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        aDialog.show();
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
        QR.bitcoinQrDialog(getActivity(), address.getAddress()).show();
    }

    public abstract CharSequence getTitle();
}
