package crea.wallet.lite.util;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by ander on 2/03/16.
 */
public class Sha384Hash implements Serializable, Comparable<Sha384Hash> {

    private static final String TAG = "Sha384Hash";

    private static final int LENGTH = 48;

    public static final Sha384Hash ZERO_HASH = wrap(new byte[LENGTH]);

    private byte[] bytes;

    public Sha384Hash(byte[] rawHashBytes) {
        Preconditions.checkArgument(rawHashBytes.length == LENGTH);
        this.bytes = rawHashBytes;
    }

    public static Sha384Hash wrap(byte[] rawHashBytes) {
        return new Sha384Hash(rawHashBytes);
    }

    public static Sha384Hash wrap(String hexString) {
        return wrap(org.creacoinj.core.Utils.HEX.decode(hexString));
    }

    public static Sha384Hash wrapReversed(byte[] rawHashBytes) {
        return wrap(org.creacoinj.core.Utils.reverseBytes(rawHashBytes));
    }

    public static Sha384Hash create(byte[] contents) {
        return of(contents);
    }

    public static Sha384Hash of(byte[] contents) {
        return wrap(hash(contents));
    }

    public static Sha384Hash createDouble(byte[] contents) {
        return twiceOf(contents);
    }

    public static Sha384Hash twiceOf(byte[] contents) {
        return wrap(hashTwice(contents));
    }

    public static Sha384Hash of(File file) throws IOException {

        Sha384Hash var3;
        FileInputStream in = new FileInputStream(file);
        var3 = of(ByteStreams.toByteArray(in));
        return var3;
    }

    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-384");
        } catch (NoSuchAlgorithmException var1) {
            throw new RuntimeException(var1);
        }
    }

    public static byte[] hash(byte[] input) {
        return hash(input, 0, input.length);
    }

    public static byte[] hash(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest();
    }

    public static byte[] hashTwice(byte[] input) {
        return hashTwice(input, 0, input.length);
    }

    public static byte[] hashTwice(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest(digest.digest());
    }

    public static byte[] hashTwice(byte[] input1, int offset1, int length1, byte[] input2, int offset2, int length2) {
        MessageDigest digest = newDigest();
        digest.update(input1, offset1, length1);
        digest.update(input2, offset2, length2);
        return digest.digest(digest.digest());
    }

    public BigInteger toBigInteger() {
        return new BigInteger(1, this.bytes);
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public byte[] getReversedBytes() {
        return org.creacoinj.core.Utils.reverseBytes(this.bytes);
    }

    @Override
    public String toString() {
        return org.creacoinj.core.Utils.HEX.encode(this.bytes);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o != null && this.getClass() == o.getClass() && Arrays.equals(this.bytes, ((Sha384Hash) o).bytes));
    }

    @Override
    public int compareTo(Sha384Hash another) {
        return 0;
    }
}
