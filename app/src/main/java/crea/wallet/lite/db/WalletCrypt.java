package crea.wallet.lite.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import crea.wallet.lite.util.Utils;

/**
 * Created by Andersson G. Acosta on 3/05/17.
 */

@Table(name = "wallet_crypt")
public class WalletCrypt extends Model {

    @Column(name = "salt", notNull = true)
    private String salt;

    @Column(name = "nonce", notNull = true)
    private int nonce;

    public String getSalt() {
        return salt;
    }

    public int getNonce() {
        return nonce;
    }

    public String generate(String pass) {
        for (int x = 0; x < nonce; x++) {
            try {
                pass = Utils.encryptInSHA2(salt + pass + salt, 1);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        }

        return pass;
    }

    @Override
    public String toString() {
        return "WalletCrypt [" +
                "salt: " + salt + ", " +
                "nonce: " + nonce + "]";
    }

    public static WalletCrypt getInstance() {
        return new Select().from(WalletCrypt.class).executeSingle();
    }

    public static WalletCrypt random() {
        WalletCrypt walletCrypt = new WalletCrypt();
        byte[] saltBytes = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.setSeed(Utils.getRandom(0L, Long.MAX_VALUE));
        sr.nextBytes(saltBytes);
        sr = null;
        walletCrypt.salt = org.creativecoinj.core.Utils.HEX.encode(saltBytes);
        walletCrypt.nonce = Utils.getRandom(100, 1000);
        return walletCrypt;
    }
}
