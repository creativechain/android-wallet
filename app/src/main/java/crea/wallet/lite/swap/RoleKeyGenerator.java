package crea.wallet.lite.swap;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crea.wallet.lite.swap.crypto.ECKey;
import crea.wallet.lite.swap.crypto.PrivateKeyType;
import crea.wallet.lite.swap.crypto.Sha256Hash;
import crea.wallet.lite.swap.exception.KeyHandlingException;


/**
 * Created by ander on 15/02/19.
 */
public class RoleKeyGenerator {

    private AccountName username;
    private String password;

    private List<PrivateKeyType> roles;

    public RoleKeyGenerator(AccountName username, String password) {
        this.username = username;
        this.password = password;
        this.roles = new ArrayList<>();
    }

    public void addRoles(PrivateKeyType... roles) {
        this.roles.addAll(Arrays.asList(roles));
    }

    public Map<PrivateKeyType, ECKey> generateKeys() throws KeyHandlingException {
        Map<PrivateKeyType, ECKey> keyMap = new HashMap<>();

        for (PrivateKeyType type : roles) {
            String seed = username.getName() + type.toString().toLowerCase() + password;
            String brainkey = TextUtils.join(" ", seed.trim().split("[\t\n\f\r]+"));
            Sha256Hash hash = Sha256Hash.of(brainkey.getBytes());
            byte[] hashBytes = hash.getBytes();
            byte[] privKey = new byte[hashBytes.length];
            //privKey[0] = (byte) 0x80;
            System.arraycopy(hashBytes, 0, privKey, 0, hashBytes.length);

            keyMap.put(type, ECKey.fromPrivate(privKey));
        }
        return  keyMap;
    }
}
