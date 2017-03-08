package crea.wallet.lite.wallet;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.creacoinj.core.Address;
import org.creacoinj.core.AddressFormatException;
import org.creacoinj.core.NetworkParameters;

/**
 * Created by ander on 6/04/16.
 */
public class AddressUtil {

    private static final String TAG = "AddressUtil";

    public static Address fromBase58(@Nullable NetworkParameters params, @NonNull String address) {
        try {
            return new Address(params, address);
        } catch (AddressFormatException e) {
            e.printStackTrace();
            return null;
        }
    }
}
