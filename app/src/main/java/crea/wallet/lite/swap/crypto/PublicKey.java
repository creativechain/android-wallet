package crea.wallet.lite.swap.crypto;

import android.util.Log;

import java.util.Arrays;


import org.spongycastle.crypto.digests.RIPEMD160Digest;

import com.google.common.primitives.Bytes;

import crea.wallet.lite.swap.protocol.AddressPrefix;
import crea.wallet.lite.swap.base58.Base58;
import crea.wallet.lite.swap.exception.AddressFormatException;
import crea.wallet.lite.swap.exception.InvalidTransactionException;

/**
 * This class is the java implementation of the <a href=
 * "https://github.com/steemit/steem/blob/master/libraries/protocol/include/steemit/protocol/types.hpp">Steem
 * public_key object</a>.
 * 
 * @author <a href="http://steemit.com/@dez1337">dez1337</a>
 */
public class PublicKey implements ByteTransformable {

    //private static final Logger LOGGER = new Logger(PublicKey.class);
    private static final String TAG = "PublicKey";

    private static final int CHECKSUM_BYTES = 4;

    private ECKey publicKey;
    private String prefix;

    /**
     * Create a new public key by providing an address as String.
     * 
     * @param address
     *            The address in its String representation.
     *            <p>
     *            Example: <br>
     *            STM5jYVokmZHdEpwo5oCG3ES2Ca4VYzy6tM8pWWkGdgVnwo2mFLFq
     *            </p>
     * @throws AddressFormatException
     *             If the input is not base 58 or the checksum does not
     *             validate.
     */
    public PublicKey(String address) {
        // As this method is also used for parsing different operations where
        // the field could be empty we sadly have to handle "null" cases here.
        if (address != null && !"".equals(address)) {
            if (address.length() != 53) {
                Log.w(TAG, "The provided address '" + address +"' has an invalid length and will not be set.");
                this.setPublicKey(null);
            } else {
                // We expect the first three chars to be the prefix (STM). The
                // rest
                // of the String contains the base58 encoded public key and its
                // checksum.
                this.prefix = address.substring(0, 3);
                byte[] decodedAddress = Base58.decode(address.substring(3, address.length()));
                // As sha256 is used for Bitcoin and ripemd160 for Steem, we
                // can't
                // use Bitcoinjs Base58.decodeChecked here and have to do all
                // stuff
                // on our own.
                byte[] potentialPublicKey = Arrays.copyOfRange(decodedAddress, 0,
                        decodedAddress.length - CHECKSUM_BYTES);
                byte[] expectedChecksum = Arrays.copyOfRange(decodedAddress, decodedAddress.length - CHECKSUM_BYTES,
                        decodedAddress.length);

                byte[] actualChecksum = calculateChecksum(potentialPublicKey);

                // And compare them.
                for (int i = 0; i < expectedChecksum.length; i++) {
                    if (expectedChecksum[i] != actualChecksum[i]) {
                        throw new AddressFormatException("Checksum does not match.");
                    }
                }

                this.setPublicKey(ECKey.fromPublicOnly(potentialPublicKey));
            }
        } else {
            Log.w(TAG, "An empty address has been provided. This can cause some problems if you plan to broadcast this key.");
            this.setPublicKey(null);
        }
    }

    /**
     * Generate the actual checksum of a Steem public key.
     * 
     * @param publicKey
     *            The public key.
     * @return The actual checksum of a Steem public key.
     */
    private byte[] calculateChecksum(byte[] publicKey) {
        RIPEMD160Digest ripemd160Digest = new RIPEMD160Digest();
        ripemd160Digest.update(publicKey, 0, publicKey.length);
        byte[] actualChecksum = new byte[ripemd160Digest.getDigestSize()];
        ripemd160Digest.doFinal(actualChecksum, 0);
        return actualChecksum;
    }

    /**
     * Create a new public key by providing a ECKey object containing the public
     * key.
     * 
     * @param publicKey
     *            The public key.
     */
    public PublicKey(ECKey publicKey) {
        this.setPublicKey(publicKey);
        this.prefix = AddressPrefix.CREA.toString().toUpperCase();
    }

    /**
     * Recreate the address from the public key.
     * 
     * @return The address.
     */
    public String getAddressFromPublicKey() {
        try {
            // Recreate the address from the public key.
            return this.prefix + Base58.encode(Bytes.concat(this.toByteArray(),
                    Arrays.copyOfRange(calculateChecksum(this.toByteArray()), 0, CHECKSUM_BYTES)));
        } catch (InvalidTransactionException | NullPointerException e) {
            Log.d(TAG, "An error occured while generating an address from a public key.", e);
            return "";
        }
    }

    /**
     * Get the public key stored in this object.
     * 
     * @return The public key.
     */
    public ECKey getPublicKey() {
        return publicKey;
    }

    /**
     * Set the public key that should be stored in this object.
     * 
     * @param publicKey
     *            The public key.
     */
    private void setPublicKey(ECKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public byte[] toByteArray() throws InvalidTransactionException {
        if (this.getPublicKey().isCompressed()) {
            return this.getPublicKey().getPubKey();
        } else {
            return ECKey.fromPublicOnly(ECKey.compressPoint(this.getPublicKey().getPubKeyPoint())).getPubKey();
        }
    }

    @Override
    public boolean equals(Object otherPublicKey) {
        if (this == otherPublicKey)
            return true;
        if (otherPublicKey == null || !(otherPublicKey instanceof PublicKey))
            return false;
        PublicKey otherKey = (PublicKey) otherPublicKey;
        return this.getPublicKey().equals(otherKey.getPublicKey());
    }

    @Override
    public int hashCode() {
        return this.getPublicKey().hashCode();
    }
}
