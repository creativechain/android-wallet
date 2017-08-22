package crea.wallet.lite.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.creativecoinj.crypto.KeyCrypter;
import org.creativecoinj.crypto.KeyCrypterException;
import org.creativecoinj.crypto.KeyCrypterScrypt;
import org.creativecoinj.wallet.Wallet;
import org.spongycastle.crypto.params.KeyParameter;

import crea.wallet.lite.application.Constants;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static crea.wallet.lite.application.Constants.WALLET.SCRYPT_ITERATIONS_TARGET;

public abstract class DeriveKeyTask {
    private static final String TAG = "DeriveKeyTask";

    private final Handler backgroundHandler;
    private final Handler callbackHandler;

    public DeriveKeyTask(final Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
        this.callbackHandler = new Handler(Looper.myLooper());
    }

    public final void deriveKey(final Wallet wallet, final String password) {
        checkState(wallet.isEncrypted());
        final KeyCrypter keyCrypter = checkNotNull(wallet.getKeyCrypter());

        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                org.creativecoinj.core.Context.propagate(Constants.WALLET.CONTEXT);

                // Key derivation takes time.
                KeyParameter key = keyCrypter.deriveKey(password);
                boolean wasChanged = false;

                // If the key isn't derived using the desired parameters, derive a new key.
                if (keyCrypter instanceof KeyCrypterScrypt) {
                    final long scryptIterations = ((KeyCrypterScrypt) keyCrypter).getScryptParameters().getN();

                    Log.i(TAG, String.format("upgrading scrypt iterations from %1$d to %2$d; re-encrypting wallet", scryptIterations,
                            SCRYPT_ITERATIONS_TARGET));

                    final KeyCrypterScrypt newKeyCrypter = new KeyCrypterScrypt(SCRYPT_ITERATIONS_TARGET);
                    final KeyParameter newKey = newKeyCrypter.deriveKey(password);

                    // Re-encrypt wallet with new key.
                    try {
                        wallet.changeEncryptionKey(newKeyCrypter, key, newKey);
                        key = newKey;
                        wasChanged = true;
                        Log.i(TAG, "scrypt upgrade succeeded");
                    } catch (final KeyCrypterException x) {
                        Log.i(TAG, String.format("scrypt upgrade failed: %1$s", x.getMessage()));
                    }
                }

                // Hand back the (possibly changed) encryption key.
                final KeyParameter keyToReturn = key;
                final boolean keyToReturnWasChanged = wasChanged;
                callbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onSuccess(keyToReturn, keyToReturnWasChanged);
                    }
                });
            }
        });
    }

    protected abstract void onSuccess(KeyParameter encryptionKey, boolean changed);
}