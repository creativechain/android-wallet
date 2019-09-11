/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package crea.wallet.lite.swap.crypto;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;

/**
 * A Sha256Hash just wraps a <code>byte[]</code> so that equals and hash code
 * work correctly, allowing it to be used as keys in a map. It also checks that
 * the length is correct and provides a bit more type safety.
 */
public class Sha256Hash implements Serializable, Comparable<Sha256Hash> {
    /** Generated serial version UID. */
    private static final long serialVersionUID = -6235942405977272689L;
    /** The allowed length of the wrapped byte array. */
    public static final int LENGTH = 32;
    /** A {@link Sha256Hash} that only consists of 0s. */
    public static final Sha256Hash ZERO_HASH = wrap(new byte[LENGTH]);

    /** The <code>byte[]</code> wrapped by this instance. */
    private final byte[] bytes;

    /**
     * Creates a new instance that wraps the given hash value.
     * 
     * @param rawHashBytes
     *            the raw hash bytes to wrap
     * @throws IllegalArgumentException
     *             if the given array length is not exactly 32
     */
    private Sha256Hash(byte[] rawHashBytes) {
        checkArgument(rawHashBytes.length == LENGTH);
        this.bytes = rawHashBytes;
    }

    /**
     * Creates a new instance that wraps the given hash value.
     *
     * @param rawHashBytes
     *            the raw hash bytes to wrap
     * @return a new instance
     * @throws IllegalArgumentException
     *             if the given array length is not exactly 32
     */
    private static Sha256Hash wrap(byte[] rawHashBytes) {
        return new Sha256Hash(rawHashBytes);
    }

    /**
     * Creates a new instance that wraps the given hash value (represented as a
     * hex string).
     *
     * @param hexString
     *            a hash value represented as a hex string
     * @return a new instance
     * @throws IllegalArgumentException
     *             if the given string is not a valid hex string, or if it does
     *             not represent exactly 32 bytes
     */
    public static Sha256Hash wrap(String hexString) {
        return wrap(CryptoUtils.HEX.decode(hexString));
    }

    /**
     * Creates a new instance that wraps the given hash value, but with byte
     * order reversed.
     *
     * @param rawHashBytes
     *            the raw hash bytes to wrap
     * @return a new instance
     * @throws IllegalArgumentException
     *             if the given array length is not exactly 32
     */
    public static Sha256Hash wrapReversed(byte[] rawHashBytes) {
        return wrap(CryptoUtils.reverseBytes(rawHashBytes));
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the
     * given bytes.
     *
     * @param contents
     *            the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     */
    public static Sha256Hash of(byte[] contents) {
        return wrap(hash(contents));
    }

    /**
     * Creates a new instance containing the hash of the calculated hash of the
     * given bytes.
     *
     * @param contents
     *            the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (two-time) hash
     */
    public static Sha256Hash twiceOf(byte[] contents) {
        return wrap(hashTwice(contents));
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the
     * given file's contents.
     *
     * The file contents are read fully into memory, so this method should only
     * be used with small files.
     *
     * @param file
     *            the file on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     * @throws IOException
     *             if an error occurs while reading the file
     */
    public static Sha256Hash of(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            return of(ByteStreams.toByteArray(in));
        } finally {
            in.close();
        }
    }

    /**
     * Returns a new SHA-256 MessageDigest instance.
     *
     * This is a convenience method which wraps the checked exception that can
     * never occur with a RuntimeException.
     *
     * @return a new SHA-256 MessageDigest instance
     */
    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // Can't happen.
        }
    }

    /**
     * Calculates the SHA-256 hash of the given bytes.
     *
     * @param input
     *            the bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input) {
        return hash(input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range.
     *
     * @param input
     *            the array containing the bytes to hash
     * @param offset
     *            the offset within the array of the bytes to hash
     * @param length
     *            the number of bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest();
    }

    /**
     * Calculates the SHA-256 hash of the given bytes, and then hashes the
     * resulting hash again.
     *
     * @param input
     *            the bytes to hash
     * @return the double-hash (in big-endian order)
     */
    public static byte[] hashTwice(byte[] input) {
        return hashTwice(input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range, and then hashes the
     * resulting hash again.
     *
     * @param input
     *            the array containing the bytes to hash
     * @param offset
     *            the offset within the array of the bytes to hash
     * @param length
     *            the number of bytes to hash
     * @return the double-hash (in big-endian order)
     */
    public static byte[] hashTwice(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest(digest.digest());
    }

    /**
     * Calculates the hash of the hash on the given byte ranges. This is
     * equivalent to concatenating the two ranges and then passing the result to
     * {@link #hashTwice(byte[])}.
     * 
     * @param input1
     *            the first hash
     * @param offset1
     *            the offset of the first hash
     * @param length1
     *            the length of the first hash
     * @param input2
     *            the second hash
     * @param offset2
     *            the offset of the second hash
     * @param length2
     *            the length of the second hash
     * @return the hash of the hash on the given byte ranges
     */
    public static byte[] hashTwice(byte[] input1, int offset1, int length1, byte[] input2, int offset2, int length2) {
        MessageDigest digest = newDigest();
        digest.update(input1, offset1, length1);
        digest.update(input2, offset2, length2);
        return digest.digest(digest.digest());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return Arrays.equals(bytes, ((Sha256Hash) o).bytes);
    }

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique
     * enough to be a suitable hash code even for blocks, where the goal is to
     * try and get the first bytes to be zeros (i.e. the value as a big integer
     * lower than the target value).
     */
    @Override
    public int hashCode() {
        // Use the last 4 bytes, not the first 4 which are often zeros in
        // Bitcoin.
        return Ints.fromBytes(bytes[LENGTH - 4], bytes[LENGTH - 3], bytes[LENGTH - 2], bytes[LENGTH - 1]);
    }

    @Override
    public String toString() {
        return CryptoUtils.HEX.encode(bytes);
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     * 
     * @return the bytes interpreted as a positive integer
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, bytes);
    }

    /**
     * Returns the internal byte array, without defensively copying. Therefore
     * do NOT modify the returned array.
     * 
     * @return the internal byte array, without defensively copying
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Returns a reversed copy of the internal byte array.
     * 
     * @return a reversed copy of the internal byte array
     */
    public byte[] getReversedBytes() {
        return CryptoUtils.reverseBytes(bytes);
    }

    @Override
    public int compareTo(final Sha256Hash other) {
        for (int i = LENGTH - 1; i >= 0; i--) {
            final int thisByte = this.bytes[i] & 0xff;
            final int otherByte = other.bytes[i] & 0xff;
            if (thisByte > otherByte)
                return 1;
            if (thisByte < otherByte)
                return -1;
        }
        return 0;
    }
}