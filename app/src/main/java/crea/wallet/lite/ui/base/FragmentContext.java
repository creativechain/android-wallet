package crea.wallet.lite.ui.base;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ander on 15/06/15.
 */
public abstract class FragmentContext extends Fragment {

    private static final String TAG = "FragmentContext";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return initView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        afterInitialize(view, savedInstanceState);
        onCreateContent();
    }

    public void init(Bundle savedInstanceState) {

    }

    protected void onCreateContent() {

    }

    public abstract View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public abstract void afterInitialize(View view, Bundle savedInstanceState);

    public Drawable getDrawable(int id) {
        return getResources().getDrawable(id);
    }

    public XmlResourceParser getAnimation(int id) {
        return getResources().getAnimation(id);
    }

    public int getColor(int id) {
        return getResources().getColor(id);
    }

    public <T extends View> T  findViewById(int id) {
        return (T) getActivity().findViewById(id);
    }
    
    public void startService(Intent intentService) {
        getActivity().startService(intentService);
    }

    public void stopService(Intent name) {
        getActivity().stopService(name);
    }

    public Intent getIntent() {
        return getActivity().getIntent();
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {

    }

    public void runOnUiThread(Runnable action) {
        getActivity().runOnUiThread(action);
    }

    public void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        try {
            getActivity().registerReceiver(receiver, filter);
            Log.e(TAG, "Receiver " + receiver.getClass().getName() +  " registered");
        } catch (Exception e) {
            Log.e(TAG, "Impossible register receiver: " + e.getMessage());
        }

    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            getActivity().unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public void sendBroadcast(Intent intent) {
        getActivity().sendBroadcast(intent);
    }

    public void sendBroadcast(Intent intent, String receiverPermission) {
        getActivity().sendBroadcast(intent, receiverPermission);
    }

    protected Object getSystemService(String service) {
        return getActivity().getSystemService(service);
    }

    protected void onBackPressed() {

    }
}
