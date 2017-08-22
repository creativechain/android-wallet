/*
 * Copyright 2011-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package crea.wallet.lite.wallet;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.TypefaceSpan;
import android.util.Log;

import crea.wallet.lite.application.Constants;
import crea.wallet.lite.util.Hex;
import crea.wallet.lite.util.Iso8601Format;
import com.google.common.base.Charsets;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.AddressFormatException;
import org.creativecoinj.core.Base58;
import org.creativecoinj.core.DumpedPrivateKey;
import org.creativecoinj.core.ECKey;
import org.creativecoinj.core.NetworkParameters;
import org.creativecoinj.core.ScriptException;
import org.creativecoinj.core.Sha256Hash;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionInput;
import org.creativecoinj.core.TransactionOutput;
import org.creativecoinj.core.Utils;
import org.creativecoinj.crypto.ChildNumber;
import org.creativecoinj.crypto.DeterministicKey;
import org.creativecoinj.crypto.HDKeyDerivation;
import org.creativecoinj.script.Script;
import org.creativecoinj.wallet.DeterministicKeyChain;
import org.creativecoinj.wallet.KeyChain;
import org.creativecoinj.wallet.KeyChainGroup;
import org.creativecoinj.wallet.UnreadableWalletException;
import org.creativecoinj.wallet.Wallet;
import org.creativecoinj.wallet.WalletProtobufSerializer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * @author Andreas Schildbach
 */
public class WalletUtils {
	private static final String TAG = "WalletUtils";

	public static final int TRANSACTION_INPUTS = 0;
	public static final int TRANSACTION_OUTPUTS = 1;

	public static Editable formatAddress(final Address address, final int groupSize, final int lineSize) {
		return formatHash(address.toString(), groupSize, lineSize);
	}

	public static Editable formatAddress(@Nullable final String prefix, final Address address, final int groupSize, final int lineSize) {
		return formatHash(prefix, address.toString(), groupSize, lineSize, Constants.CHAR_THIN_SPACE);
	}

	public static Editable formatHash(final String address, final int groupSize, final int lineSize) {
		return formatHash(null, address, groupSize, lineSize, Constants.CHAR_THIN_SPACE);
	}

	public static long longHash(final Sha256Hash hash) {
		final byte[] bytes = hash.getBytes();

		return (bytes[31] & 0xFFl) | ((bytes[30] & 0xFFl) << 8) | ((bytes[29] & 0xFFl) << 16) | ((bytes[28] & 0xFFl) << 24)
				| ((bytes[27] & 0xFFl) << 32) | ((bytes[26] & 0xFFl) << 40) | ((bytes[25] & 0xFFl) << 48) | ((bytes[23] & 0xFFl) << 56);
	}

	public static Editable formatHash(@Nullable final String prefix, final String address, final int groupSize, final int lineSize,
									  final char groupSeparator) {
		final SpannableStringBuilder builder = prefix != null ? new SpannableStringBuilder(prefix) : new SpannableStringBuilder();

		final int len = address.length();
		for (int i = 0; i < len; i += groupSize) {
			final int end = i + groupSize;
			final String part = address.substring(i, end < len ? end : len);

			builder.append(part);
			builder.setSpan(new TypefaceSpan("monospace"), builder.length() - part.length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			if (end < len)
			{
				final boolean endOfLine = lineSize > 0 && end % lineSize == 0;
				builder.append(endOfLine ? '\n' : groupSeparator);
			}
		}

		return builder;
	}

	@Nullable
	public static Address getToAddressOfSent(final Transaction tx, final Wallet wallet) {
		for (final TransactionOutput output : tx.getOutputs()) {
			try	{
				if (!output.isMine(wallet))	{
					final Script script = output.getScriptPubKey();
					return script.getToAddress(Constants.WALLET.NETWORK_PARAMETERS, true);
				}
			} catch (final ScriptException x) {
				// swallow
			}
		}

		return null;
	}

	@Nullable
	public static Address[] getWalletAddressOfReceived(final Transaction tx, final Wallet wallet)	{
		List<Address> addresses = new ArrayList<>();
		for (final TransactionOutput output : tx.getOutputs()) {
			try	{
				if (output.isMine(wallet))	{
					final Script script = output.getScriptPubKey();
					addresses.add(script.getToAddress(Constants.WALLET.NETWORK_PARAMETERS, true));
				}
			} catch (final ScriptException x) {
				// swallow
			}
		}

		Address[] addr = new Address[0];
		return addresses.toArray(addr);
	}

	public static String[] getAddressStrings(List<Address> addresses) {
		String[] strings = new String[addresses.size()];
		for (int x = 0; x < addresses.size(); x++) {
			Log.i(TAG, "Getting " + addresses.get(x));
			strings[x] = addresses.get(x).toString();
		}

		Log.i(TAG, "Getting Addresses: " + Arrays.toString(strings));
		return strings;
	}

	public static String[] getAddressStrings(Address... addresses) {
		return getAddressStrings(Arrays.asList(addresses));
	}

	public static String[] getAddressStrings(Transaction tx, int txPart, boolean cashIn) {
		switch (txPart) {
			case TRANSACTION_INPUTS:
				return getAddressStrings(getInputAddresses(tx, cashIn));
			case TRANSACTION_OUTPUTS:
				return getAddressStrings(getOutputAddresses(tx, cashIn));
		}

		return null;
	}

	public static List<Address> getAddressFromTx(Transaction tx, int txPart, boolean cashIn) {
		ArrayList<Address> addresses = new ArrayList<>();

		Wallet wallet = WalletHelper.INSTANCE.getWallet();
		switch (txPart) {
			case TRANSACTION_INPUTS:
				for (TransactionInput i : tx.getInputs()) {
					Address a = i.getFromAddress();
					boolean isMine = wallet.isPubKeyMine(a.getHash160()) || wallet.isPubKeyHashMine(a.getHash160());

					if (cashIn && !isMine || !cashIn && isMine) {
						addresses.add(a);
					}

				}
				break;
			case TRANSACTION_OUTPUTS:
				for (TransactionOutput o : tx.getOutputs()) {
					if (cashIn && o.isMine(wallet) || !cashIn && !o.isMine(wallet)) {
						Address a = o.getScriptPubKey().getToAddress(Constants.WALLET.NETWORK_PARAMETERS);
						addresses.add(a);
					}
				}
		}
		return addresses;
	}

	public static List<Address> getInputAddresses(Transaction tx, boolean cashIn) {
		return getAddressFromTx(tx, TRANSACTION_INPUTS, cashIn);
	}

	public static List<Address> getOutputAddresses(Transaction tx, boolean cashIn) {
		return getAddressFromTx(tx, TRANSACTION_OUTPUTS, cashIn);
	}

	public static Wallet restoreWalletFromProtobufOrBase58(final InputStream is, final NetworkParameters expectedNetworkParameters) throws IOException
	{
		is.mark((int) Constants.BACKUP_MAX_CHARS);

		try
		{
			return restoreWalletFromProtobuf(is, expectedNetworkParameters);
		}
		catch (final IOException x)
		{
			try
			{
				is.reset();
				return restorePrivateKeysFromBase58(is, expectedNetworkParameters);
			}
			catch (final IOException x2)
			{
				throw new IOException("cannot read protobuf (" + x.getMessage() + ") or base58 (" + x2.getMessage() + ")", x);
			}
		}
	}

	public static Wallet restoreWalletFromProtobuf(final InputStream is, final NetworkParameters expectedNetworkParameters) throws IOException
	{
		try
		{
			final Wallet wallet = new WalletProtobufSerializer().readWallet(is);

			if (!wallet.getParams().equals(expectedNetworkParameters))
				throw new IOException("bad wallet network parameters: " + wallet.getParams().getId());

			return wallet;
		}
		catch (final UnreadableWalletException x)
		{
			throw new IOException("unreadable wallet", x);
		}
	}

	public static Wallet restorePrivateKeysFromBase58(final InputStream is, final NetworkParameters expectedNetworkParameters) throws IOException
	{
		final BufferedReader keyReader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));

		// create non-HD wallet
		final KeyChainGroup group = new KeyChainGroup(expectedNetworkParameters);
		group.importKeys(WalletUtils.readKeys(keyReader, expectedNetworkParameters));
		return new Wallet(expectedNetworkParameters, group);
	}

	public static void writeKeys(final Writer out, final List<ECKey> keys) throws IOException
	{
		final DateFormat format = Iso8601Format.newDateTimeFormatT();

		out.write("# KEEP YOUR PRIVATE KEYS SAFE! Anyone who can read this can spend your Bitcoins.\n");

		for (final ECKey key : keys)
		{
			out.write(key.getPrivateKeyEncoded(Constants.WALLET.NETWORK_PARAMETERS).toString());
			if (key.getCreationTimeSeconds() != 0)
			{
				out.write(' ');
				out.write(format.format(new Date(key.getCreationTimeSeconds() * DateUtils.SECOND_IN_MILLIS)));
			}
			out.write('\n');
		}
	}

	public static List<ECKey> readKeys(final BufferedReader in, final NetworkParameters expectedNetworkParameters) throws IOException
	{
		try
		{
			final DateFormat format = Iso8601Format.newDateTimeFormatT();

			final List<ECKey> keys = new LinkedList<ECKey>();

			long charCount = 0;
			while (true)
			{
				final String line = in.readLine();
				if (line == null)
					break; // eof
				charCount += line.length();
				if (charCount > Constants.BACKUP_MAX_CHARS)
					throw new IOException("read more than the limit of " + Constants.BACKUP_MAX_CHARS + " characters");
				if (line.trim().isEmpty() || line.charAt(0) == '#')
					continue; // skip comment

				final String[] parts = line.split(" ");

				final ECKey key = new DumpedPrivateKey(expectedNetworkParameters, parts[0]).getKey();
				key.setCreationTimeSeconds(parts.length >= 2 ? format.parse(parts[1]).getTime() / DateUtils.SECOND_IN_MILLIS : 0);

				keys.add(key);
			}

			return keys;
		}
		catch (final AddressFormatException x)
		{
			throw new IOException("cannot read keys", x);
		}
		catch (final ParseException x)
		{
			throw new IOException("cannot read keys", x);
		}
	}

	public static final FileFilter KEYS_FILE_FILTER = new FileFilter()
	{
		@Override
		public boolean accept(final File file)
		{
			BufferedReader reader = null;

			try
			{
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8));
				WalletUtils.readKeys(reader, Constants.WALLET.NETWORK_PARAMETERS);

				return true;
			}
			catch (final IOException x)
			{
				return false;
			}
			finally
			{
				if (reader != null)
				{
					try
					{
						reader.close();
					}
					catch (final IOException x)
					{
						// swallow
					}
				}
			}
		}
	};

	public static final FileFilter BACKUP_FILE_FILTER = new FileFilter()
	{
		@Override
		public boolean accept(final File file)
		{
			InputStream is = null;

			try
			{
				is = new FileInputStream(file);
				return WalletProtobufSerializer.isWallet(is);
			}
			catch (final IOException x)
			{
				return false;
			}
			finally
			{
				if (is != null)
				{
					try
					{
						is.close();
					}
					catch (final IOException x)
					{
						// swallow
					}
				}
			}
		}
	};

	public static byte[] walletToByteArray(final Wallet wallet)
	{
		try
		{
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			new WalletProtobufSerializer().writeWallet(wallet, os);
			os.close();
			return os.toByteArray();
		}
		catch (final IOException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static Wallet walletFromByteArray(final byte[] walletBytes) {
		try
		{
			final ByteArrayInputStream is = new ByteArrayInputStream(walletBytes);
			final Wallet wallet = new WalletProtobufSerializer().readWallet(is);
			is.close();
			return wallet;
		}
		catch (final UnreadableWalletException x)
		{
			throw new RuntimeException(x);
		}
		catch (final IOException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static Address newAddressOrThrow(final NetworkParameters params, final String base58) throws IllegalArgumentException
	{
		try
		{
			return new Address(params, base58);
		}
		catch (AddressFormatException x)
		{
			throw new IllegalArgumentException(x);
		}
	}

	public static boolean isValidAddress(NetworkParameters params, String address) {
		try {
			newAddressOrThrow(params, address);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isPayToManyTransaction(final Transaction transaction)	{
		return transaction.getOutputs().size() > 20;
	}

	public static Address generateAddress(Wallet wallet, int index) {
		DeterministicKeyChain dkc = wallet.getActiveKeyChain();
		DeterministicKey extendedKey = dkc.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);

		byte[] derived = HDKeyDerivation.deriveChildKeyBytesFromPublic(extendedKey, new ChildNumber(index, false), HDKeyDerivation.PublicDeriveMode.WITH_INVERSION).keyBytes;
		byte[] ripmd160Hash = Utils.sha256hash160(derived);
		byte[] mainHash = new byte[ripmd160Hash.length + 1];
		String netWorkId = Constants.TEST ? "6f" : "00";
		mainHash[0] = Hex.decodeHex(netWorkId)[0];
		System.arraycopy(ripmd160Hash, 0, mainHash, 1, ripmd160Hash.length);

		byte[] main256Hash = mainHash;
		main256Hash = Sha256Hash.hash(main256Hash);
		main256Hash = Sha256Hash.hash(main256Hash);

		byte[] checksum = new byte[4];
		System.arraycopy(main256Hash, 0, checksum, 0, checksum.length);

		byte[] preAddress = new byte[mainHash.length + 4];
		System.arraycopy(mainHash, 0, preAddress, 0, mainHash.length);
		System.arraycopy(checksum, 0, preAddress, mainHash.length, checksum.length);

		String address = Base58.encode(preAddress);
		return Address.fromBase58(wallet.getParams(), address);
	}

	public static Address generateAddress(Wallet wallet) {
		return generateAddress(wallet, wallet.getIssuedReceiveAddresses().size() +1);
	}

	public static List<Address> generateAddresses(Wallet wallet, int fromIndex, int numOfAddresses) {
		List<Address> list = new ArrayList<>();
		for (int x = 0; x < numOfAddresses; x++) {
			list.add(generateAddress(wallet, fromIndex + x));
		}

		return list;
	}

	public static List<Address> generateAddresses(Wallet wallet, int numOfAddresses) {
		return generateAddresses(wallet, 0, numOfAddresses);
	}

	public static List<String> parseMnemonicCode(String mnemonic) {
		String[] stringArray = mnemonic.split(" ");
		return Arrays.asList(stringArray);
	}
}
