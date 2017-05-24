package crea.wallet.lite.ui.main;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Toast;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.background.WalletExporter;
import crea.wallet.lite.util.DialogFactory;
import crea.wallet.lite.util.IntentUtils;
import crea.wallet.lite.util.QR;

import com.activeandroid.query.Delete;
import com.chip_chap.services.task.Task;
import com.chip_chap.services.transaction.Btc2BtcTransaction;

import static android.app.Activity.RESULT_OK;

/**
 * Created by ander on 17/11/16.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = "SettingsFragment";

    private Configuration conf = Configuration.getInstance();
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        ListPreference feepreference = (ListPreference) findPreference("transaction_fee");
        if (feepreference.getValue() == null) {
            feepreference.setValueIndex(1);
        }

        final ListPreference priceIntervalPref = (ListPreference) findPreference("price_update_interval");
        priceIntervalPref.setSummary(priceIntervalPref.getEntry());
        priceIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                CharSequence[] values = priceIntervalPref.getEntryValues();
                CharSequence[] entries = priceIntervalPref.getEntries();
                int index = 0;
                for (CharSequence c : values) {
                    if (c.equals(newValue)) {
                        break;
                    }
                    index++;
                }
                priceIntervalPref.setSummary(entries[index]);
                return true;
            }
        });

        final Preference seedPreference = findPreference("export_seed");
        seedPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                IntentUtils.checkPin(getActivity());
                return true;
            }
        });

        final SwitchPreferenceCompat deleteBlockchain = (SwitchPreferenceCompat) findPreference("delete_blockchain");
        deleteBlockchain.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                deleteBlockchain.setChecked(false);
                conf.setDeletingBlockchain(false);
                AlertDialog ad = DialogFactory.warn(getActivity(), getString(R.string.restart_blockchain), getString(R.string.delete_blockchain_warning));
                ad.setButton(AlertDialog.BUTTON_POSITIVE, getActivity().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        WalletApplication.INSTANCE.resetBlockchain();
                        Toast.makeText(getActivity(), R.string.blockchain_restarting, Toast.LENGTH_SHORT).show();
                        WalletApplication.INSTANCE.startBlockchainService(true);
                        deleteBlockchain.setChecked(true);
                        conf.setDeletingBlockchain(true);
                        deleteBlockchain.setEnabled(false);
                        deleteBlockchain.setSummary(getString(R.string.synchronization_in_progress));
                        new Delete().from(Btc2BtcTransaction.class).execute();
                    }
                });

                ad.setButton(AlertDialog.BUTTON_NEGATIVE, getActivity().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        deleteBlockchain.setChecked(false);
                        conf.setDeletingBlockchain(false);
                        deleteBlockchain.setEnabled(true);
                    }
                });
                ad.show();
                return true;
            }
        });
    }

    private void exportSeed() {
        final ProgressDialog pDialog = DialogFactory.progress(getActivity(), R.string.mnemonic_code, R.string.getting_mnemonic_code);
        pDialog.setCancelable(false);
        pDialog.show();
        new WalletExporter(Configuration.getInstance().getPin(), new Task<Bundle>() {
            @Override
            public void doTask(Bundle data) {
                pDialog.dismiss();
                if (data != null && !data.isEmpty()) {
                    final String seed = data.getString("exported");
                    showMnemonicDialog(seed);
                }
            }
        }, WalletExporter.MNEMONIC_CODE).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showMnemonicDialog(final String seed) {
        AlertDialog aDialog = DialogFactory.alert(getActivity(), R.string.mnemonic_code, seed);
        aDialog.setMessage(seed);
        aDialog.setCancelable(false);
        aDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        aDialog.setButton(AlertDialog.BUTTON_POSITIVE,getResources().getString(R.string.to_qr_code), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                Bitmap qr = QR.fromString(seed);
                QR.getCoinQrDialog(getActivity(), qr, null).show();
            }
        });
        aDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                exportSeed();
            }
        }
    }
}
