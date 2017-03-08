package crea.wallet.lite.ui.address;

import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import crea.wallet.lite.R;
import crea.wallet.lite.ui.base.AddressBookFragment;

public class AddressBookActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final TabLayout tabs = (TabLayout) findViewById(R.id.tabs);

        AddressBookFragmentAdapter tabAdapter = new AddressBookFragmentAdapter(getSupportFragmentManager(),
                new WalletAddressesFragment(),
                new ContactAddressesFragment()
        );

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(tabAdapter);
        tabs.setTabMode(TabLayout.MODE_FIXED);
        tabs.setupWithViewPager(viewPager);

    }

    public class AddressBookFragmentAdapter extends FragmentStatePagerAdapter {

        private static final String TAG = "ChatFragmentAdapter";

        private AddressBookFragment[] fragments;
        public AddressBookFragmentAdapter(FragmentManager fm, AddressBookFragment... fragments) {
            super(fm);
            this.fragments = fragments;
        }

        public void addFragment(AddressBookFragment fragment) {
            AddressBookFragment[] frs = new AddressBookFragment[fragments.length + 1];
            System.arraycopy(fragments, 0, frs, 0, fragments.length);
            frs[frs.length - 1] = fragment;
            fragments = frs;
        }

        @Override
        public AddressBookFragment getItem(int position) {
            AddressBookFragment fragment = fragments[position];
            return fragments[position];
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragments[position].getTitle();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
