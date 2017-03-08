package crea.wallet.lite.ui.main;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import crea.wallet.lite.R;

public class SettingsActivity extends AppCompatActivity {

    private SettingsFragment settingsFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        settingsFragment.onActivityResult(requestCode, resultCode, data);
    }
}
