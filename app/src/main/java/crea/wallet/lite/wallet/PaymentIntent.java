/*
 * Copyright 2014-2015 the original author or authors.
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

import android.os.Parcel;
import android.os.Parcelable;

import crea.wallet.lite.application.Constants;
import com.google.common.io.BaseEncoding;

import org.creacoinj.core.Address;
import org.creacoinj.core.Coin;
import org.creacoinj.core.ScriptException;
import org.creacoinj.core.Transaction;
import org.creacoinj.protocols.payments.PaymentProtocol;
import org.creacoinj.protocols.payments.PaymentProtocolException;
import org.creacoinj.script.Script;
import org.creacoinj.script.ScriptBuilder;
import org.creacoinj.uri.BitcoinURI;
import org.creacoinj.wallet.SendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * @author Andreas Schildbach
 */
public final class PaymentIntent implements Parcelable {
	public enum Standard {BIP21, BIP70}

	public final static class Output implements Parcelable {
		public final Coin amount;
		public final Script script;

		public Output(final Coin amount, final Script script) {
			this.amount = amount;
			this.script = script;
		}

		public static Output valueOf(final PaymentProtocol.Output output) throws PaymentProtocolException.InvalidOutputs {
			try	{
				final Script script = new Script(output.scriptData);
				return new Output(output.amount, script);
			} catch (final ScriptException x) {
				throw new PaymentProtocolException.InvalidOutputs("unparseable script in output: " + Constants.HEX.encode(output.scriptData));
			}
		}

		public boolean hasAmount()
		{
			return amount != null && amount.signum() != 0;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();

			builder.append(getClass().getSimpleName());
			builder.append('[');
			builder.append(hasAmount() ? amount.toPlainString() : "null");
			builder.append(',');
			if (script.isSentToAddress() || script.isPayToScriptHash()) {
				builder.append(script.getToAddress(Constants.WALLET.NETWORK_PARAMETERS));
			} else if (script.isSentToRawPubKey()) {
				builder.append(Constants.HEX.encode(script.getPubKey()));
			} else if (script.isSentToMultiSig()) {
				builder.append("multisig");
			} else {
				builder.append("unknown");
			}
			builder.append(']');

			return builder.toString();
		}

		@Override
		public int describeContents(){
			return 0;
		}

		@Override
		public void writeToParcel(final Parcel dest, final int flags) {
			dest.writeSerializable(amount);

			final byte[] program = script.getProgram();
			dest.writeInt(program.length);
			dest.writeByteArray(program);
		}

		public static final Creator<Output> CREATOR = new Creator<Output>() {
			@Override
			public Output createFromParcel(final Parcel in)
			{
				return new Output(in);
			}

			@Override
			public Output[] newArray(final int size)
			{
				return new Output[size];
			}
		};

		private Output(final Parcel in)	{
			amount = (Coin) in.readSerializable();

			final int programLength = in.readInt();
			final byte[] program = new byte[programLength];
			in.readByteArray(program);
			script = new Script(program);
		}
	}

	@Nullable
	public final Standard standard;

	@Nullable
	public final String payeeName;

	@Nullable
	public final String payeeVerifiedBy;

	@Nullable
	public final Output[] outputs;

	@Nullable
	public final String memo;

	@Nullable
	public final String paymentUrl;

	@Nullable
	public final byte[] payeeData;

	@Nullable
	public final String paymentRequestUrl;

	@Nullable
	public final byte[] paymentRequestHash;

	private static final Logger log = LoggerFactory.getLogger(PaymentIntent.class);

	public PaymentIntent(@Nullable final Standard standard, @Nullable final String payeeName, @Nullable final String payeeVerifiedBy,
						 @Nullable final Output[] outputs, @Nullable final String memo, @Nullable final String paymentUrl, @Nullable final byte[] payeeData,
						 @Nullable final String paymentRequestUrl, @Nullable final byte[] paymentRequestHash) {
		this.standard = standard;
		this.payeeName = payeeName;
		this.payeeVerifiedBy = payeeVerifiedBy;
		this.outputs = outputs;
		this.memo = memo;
		this.paymentUrl = paymentUrl;
		this.payeeData = payeeData;
		this.paymentRequestUrl = paymentRequestUrl;
		this.paymentRequestHash = paymentRequestHash;
	}

	private PaymentIntent(final Parcel in)	{
		standard = (Standard) in.readSerializable();

		payeeName = in.readString();
		payeeVerifiedBy = in.readString();

		final int outputsLength = in.readInt();
		if (outputsLength > 0)	{
			outputs = new Output[outputsLength];
			in.readTypedArray(outputs, Output.CREATOR);
		} else	{
			outputs = null;
		}

		memo = in.readString();

		paymentUrl = in.readString();

		final int payeeDataLength = in.readInt();
		if (payeeDataLength > 0){
			payeeData = new byte[payeeDataLength];
			in.readByteArray(payeeData);
		} else	{
			payeeData = null;
		}

		paymentRequestUrl = in.readString();

		final int paymentRequestHashLength = in.readInt();
		if (paymentRequestHashLength > 0) {
			paymentRequestHash = new byte[paymentRequestHashLength];
			in.readByteArray(paymentRequestHash);
		} else {
			paymentRequestHash = null;
		}
	}

	public static PaymentIntent fromBitcoinUri(final BitcoinURI bitcoinUri)	{
		final Address address = bitcoinUri.getAddress();
		final Output[] outputs = address != null ? buildSimplePayTo(bitcoinUri.getAmount(), address) : null;
		final String paymentRequestHashStr = (String) bitcoinUri.getParameterByName("h");
		final byte[] paymentRequestHash = paymentRequestHashStr != null ? base64UrlDecode(paymentRequestHashStr) : null;

		return new PaymentIntent(Standard.BIP21, null, null, outputs, bitcoinUri.getLabel(), null, null, bitcoinUri.getPaymentRequestUrl(), paymentRequestHash);
	}

	private static final BaseEncoding BASE64URL = BaseEncoding.base64Url().omitPadding();

	private static byte[] base64UrlDecode(final String encoded)	{
		try	{
			return BASE64URL.decode(encoded);
		} catch (final IllegalArgumentException x) {
			log.info("cannot base64url-decode: " + encoded);
			return null;
		}
	}

	public SendRequest toSendRequest()	{
		final Transaction transaction = new Transaction(Constants.WALLET.NETWORK_PARAMETERS);
		for (final Output output : outputs) {
			transaction.addOutput(output.amount, output.script);
		}
		return SendRequest.forTx(transaction);
	}

	private static Output[] buildSimplePayTo(final Coin amount, final Address address)	{
		return new Output[] { new Output(amount, ScriptBuilder.createOutputScript(address)) };
	}

	public boolean hasPayee() {
		return payeeName != null;
	}

	public boolean hasOutputs()	{
		return outputs != null && outputs.length > 0;
	}

	public boolean hasAddress() {
		if (outputs == null || outputs.length != 1)
			return false;

		final Script script = outputs[0].script;
		return script.isSentToAddress() || script.isPayToScriptHash() || script.isSentToRawPubKey();
	}

	public Address getAddress()	{
		if (!hasAddress())
			throw new IllegalStateException();

		final Script script = outputs[0].script;
		return script.getToAddress(Constants.WALLET.NETWORK_PARAMETERS, true);
	}

	public boolean hasAmount() {
		if (hasOutputs())
			for (final Output output : outputs)
				if (output.hasAmount())
					return true;

		return false;
	}

	public Coin getAmount() {
		Coin amount = Coin.ZERO;

		if (hasOutputs())
			for (final Output output : outputs)
				if (output.hasAmount())
					amount = amount.add(output.amount);

		if (amount.signum() != 0)
			return amount;
		else
			return null;
	}

	public boolean mayEditAmount() {
		return !(standard == Standard.BIP70 && hasAmount());
	}

	public boolean equalsAmount(final PaymentIntent other) {
		final boolean hasAmount = hasAmount();
		return hasAmount == other.hasAmount() && !(hasAmount && !getAmount().equals(other.getAmount()));

	}

	public boolean equalsAddress(final PaymentIntent other) {
		final boolean hasAddress = hasAddress();
		return hasAddress == other.hasAddress() && !(hasAddress && !getAddress().equals(other.getAddress()));
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();

		builder.append(getClass().getSimpleName());
		builder.append('[');
		builder.append(standard);
		builder.append(',');
		if (hasPayee())	{
			builder.append(payeeName);
			if (payeeVerifiedBy != null)
				builder.append("/").append(payeeVerifiedBy);
			builder.append(',');
		}
		builder.append(hasOutputs() ? Arrays.toString(outputs) : "null");
		builder.append(',');
		builder.append(paymentUrl);
		if (payeeData != null)	{
			builder.append(",payeeData=");
			builder.append(Constants.HEX.encode(payeeData));
		}

		if (paymentRequestUrl != null) {
			builder.append(",paymentRequestUrl=");
			builder.append(paymentRequestUrl);
		}

		if (paymentRequestHash != null)	{
			builder.append(",paymentRequestHash=");
			builder.append(Constants.HEX.encode(paymentRequestHash));
		}
		builder.append(']');

		return builder.toString();
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeSerializable(standard);

		dest.writeString(payeeName);
		dest.writeString(payeeVerifiedBy);

		if (outputs != null) {
			dest.writeInt(outputs.length);
			dest.writeTypedArray(outputs, 0);
		} else {
			dest.writeInt(0);
		}

		dest.writeString(memo);
		dest.writeString(paymentUrl);

		if (payeeData != null) {
			dest.writeInt(payeeData.length);
			dest.writeByteArray(payeeData);
		} else {
			dest.writeInt(0);
		}

		dest.writeString(paymentRequestUrl);

		if (paymentRequestHash != null)	{
			dest.writeInt(paymentRequestHash.length);
			dest.writeByteArray(paymentRequestHash);
		} else {
			dest.writeInt(0);
		}
	}

	public static final Creator<PaymentIntent> CREATOR = new Creator<PaymentIntent>()	{
		@Override
		public PaymentIntent createFromParcel(final Parcel in)
		{
			return new PaymentIntent(in);
		}

		@Override
		public PaymentIntent[] newArray(final int size)
		{
			return new PaymentIntent[size];
		}
	};
}
