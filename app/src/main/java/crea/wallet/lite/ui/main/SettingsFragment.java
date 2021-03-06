package crea.wallet.lite.ui.main;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.util.task.WalletExporter;
import crea.wallet.lite.ui.tool.PinActivity;
import crea.wallet.lite.util.wrapper.DialogFactory;
import crea.wallet.lite.util.wrapper.IntentUtils;
import crea.wallet.lite.util.wrapper.QR;
import crea.wallet.lite.util.task.Task;
import crea.wallet.lite.wallet.FeeCategory;


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

        final ListPreference feepreference = (ListPreference) findPreference("transaction_fee");
        FeeCategory category;
        if (feepreference.getValue() != null) {
            try {
                category = FeeCategory.valueOf(feepreference.getValue());
            } catch (Exception e) {
                e.printStackTrace();
                category = FeeCategory.PRIORITY;
                conf.setFeeCategory(FeeCategory.PRIORITY);
            }
        } else {
            category = conf.getFeeCategory();
        }

        String summary = feepreference.getEntry() + ", " + conf.getTransactionFee(category).longValue() + " s/Kb";
        feepreference.setSummary(summary);
        feepreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                CharSequence[] values = feepreference.getEntryValues();
                CharSequence[] entries = feepreference.getEntries();
                int index = 0;
                for (CharSequence c : values) {
                    if (c.equals(newValue)) {
                        break;
                    }
                    index++;
                }
                FeeCategory category = FeeCategory.valueOf(values[index].toString());
                Log.d(TAG, Arrays.toString(values) + ", " + Arrays.toString(entries) + ", INDEX=" + index);
                String summary = entries[index] + ", " + conf.getTransactionFee(category).longValue() + " s/Kb";
                feepreference.setSummary(summary);
                return true;
            }
        });
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
                IntentUtils.checkPin(SettingsFragment.this);
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

    private void exportSeed(String pin) {
        final ProgressDialog pDialog = DialogFactory.progress(getActivity(), R.string.mnemonic_code, R.string.getting_mnemonic_code);
        pDialog.setCancelable(false);
        pDialog.show();
        new WalletExporter(pin, new Task<Bundle>() {
            @Override
            public void doTask(Bundle data) {
                pDialog.dismiss();
                if (data != null && !data.isEmpty()) {
                    final String seed = data.getString("exported");
                    showMnemonicDialog(seed);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.cannot_export_seed), Toast.LENGTH_LONG).show();
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
                QR.getMnemonicQrDialog(getActivity(), seed).show();
            }
        });
        aDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == IntentUtils.PIN) {
                String pin = data.getStringExtra(PinActivity.EXTRA_CODE);
                exportSeed(pin);
            }
        }
    }
}
