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

package crea.wallet.lite.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import org.creativecoinj.core.AbstractCoin;
import org.creativecoinj.core.Coin;
import org.creativecoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import crea.wallet.lite.coin.CoinUtils;
import crea.wallet.lite.wallet.FeeCategory;

/**
 * @author Andreas Schildbach
 */
public class Configuration {

	private static final String TAG = "Configuration";

	private final SharedPreferences prefs;

	public static final String PREFS_KEY_BTC_PRECISION = "btc_precision";
	public static final String PREFS_KEY_CONNECTIVITY_NOTIFICATION = "connectivity_notification";
	public static final String PREFS_KEY_EXCHANGE_CURRENCY = "exchange_currency";
	public static final String PREFS_KEY_TRUSTED_PEER = "trusted_peer";
	public static final String PREFS_KEY_TRUSTED_PEER_ONLY = "trusted_peer_only";
	public static final String PREFS_KEY_REMIND_BACKUP = "remind_backup";
	public static final String PREFS_KEY_LAST_PRICE = "last_price";
	public static final String PREFS_KEY_TRANSACTION_FEE = "transaction_fee";
	public static final String PREFS_KEY_TRANSACTION_FEE_CATEGORY = "tx_fee_category";
	public static final String PREFS_KEY_IDLE_DETECTION = "idle_detection";
	private static final String PREFS_KEY_NOTIF_TRANSACTIONS = "notif_transactions";
	public static final String PREFS_KEY_DELETE_BLOCKCHAIN = "delete_blockchain";
	public static final String PREFS_KEY_MAIN_WALLET_FILE = "main_wallet_file";
	public static final String PREFS_KEY_HD_ACCOUNTS = "hd_accounts";
	public static final String PREFS_KEY_MAIN_CURRENCY = "main_currency";
	public static final String PREFS_KEY_CREA_PRICE_BTC = "crea_price_btc";
	public static final String PREFS_KEY_CREA_PRICE_EUR = "crea_price_eur";
	public static final String PREFS_KEY_CREA_PRICE_GBP = "crea_price_pln";
	public static final String PREFS_KEY_CREA_PRICE_MXN = "crea_price_mxn";
	public static final String PREFS_KEY_CREA_PRICE_USD = "crea_price_usd";
	public static final String PREFS_KEY_SHOW_EXCHANGE_VALUE = "show_exchange_value";
	public static final String PREFS_KEY_PRICE_UPDATE_INTERVAL = "price_update_interval";
	public static final String PREFS_KEY_SPEND_UNCONFIRMED_TX = "spend_unconfirmed_tx";

	private static final int PREFS_DEFAULT_BTC_SHIFT = 8;
	private static final int PREFS_DEFAULT_BTC_PRECISION = 8;
	private static final String PREFS_KEY_LAST_USED = "last_used";
	private static final String PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever";
	private static final String PREFS_KEY_PASSCODE = "PASSCODE";

	private static final Logger log = LoggerFactory.getLogger(Configuration.class);

	public Configuration(Context context) {
		this(PreferenceManager.getDefaultSharedPreferences(context));
	}

	public Configuration(final SharedPreferences prefs) {
		this.prefs = prefs;
	}

	public AbstractCoin getCreaPrice(String c) {
		String key;
		c = c.toUpperCase();
		switch (c) {
			case "USD":
				key = PREFS_KEY_CREA_PRICE_USD;
				break;
			case "GBP":
				key = PREFS_KEY_CREA_PRICE_GBP;
				break;
			case "MXN":
				key = PREFS_KEY_CREA_PRICE_MXN;
				break;
			case "BTC":
				key = PREFS_KEY_CREA_PRICE_BTC;
				break;
			default:
				key = PREFS_KEY_CREA_PRICE_EUR;
				c = "EUR";
				break;
		}
		long val = prefs.getLong(key, 0);

		return CoinUtils.valueOf(c, val);

	}

	public AbstractCoin getPriceForMainCurrency() {
		return getCreaPrice(getMainCurrency());
	}

	public void setCreaPrice(String c, long value) {
		String key = null;
		c = c.toUpperCase();
		switch (c) {
			case "USD":
			case "$":
				key = PREFS_KEY_CREA_PRICE_USD;
				break;
			case "GBP":
			case "£":
				key = PREFS_KEY_CREA_PRICE_GBP;
				break;
			case "MXN":
				key = PREFS_KEY_CREA_PRICE_MXN;
				break;
			case "EUR":
			case "€":
				key = PREFS_KEY_CREA_PRICE_EUR;
				break;
			case "BTC":
				key = PREFS_KEY_CREA_PRICE_BTC;
				break;
		}

		if (key != null) {
			Editor editor = prefs.edit();
			editor.putLong(key, value);
			editor.apply();
		}
	}

	public String getMainCurrency() {
		return prefs.getString(PREFS_KEY_MAIN_CURRENCY, "EUR");
	}

	public void setMainCurrency(String c) {
		Editor editor = prefs.edit();
		editor.putString(PREFS_KEY_MAIN_CURRENCY, c);
		editor.apply();
	}

	public void setFeeCategory(FeeCategory category) {
		prefs.edit().putString(PREFS_KEY_TRANSACTION_FEE_CATEGORY, category.toString()).apply();
	}

	public FeeCategory getFeeCategory() {
		String s = prefs.getString(PREFS_KEY_TRANSACTION_FEE, FeeCategory.PRIORITY.toString());
		return FeeCategory.valueOf(s);
	}

	public void setTransactionFee(FeeCategory category, Coin fee) {
		prefs.edit().putLong(category.toString(), fee.longValue()).apply();
	}

	public Coin getTransactionFee(FeeCategory category) {
		long fee = prefs.getLong(category.toString(), 505000);
		return Coin.valueOf(fee);
	}

	public Coin getTransactionFee() {
		FeeCategory category = getFeeCategory();
		return getTransactionFee(category);
	}

	public boolean isDeletingBlockchain() {
		return prefs.getBoolean(PREFS_KEY_DELETE_BLOCKCHAIN, false);
	}

	public void setDeletingBlockchain(boolean deleting) {
		Editor editor = prefs.edit();
		editor.putBoolean(PREFS_KEY_DELETE_BLOCKCHAIN, deleting);
		editor.apply();
	}

	public String getPin() {
		return prefs.getString(PREFS_KEY_PASSCODE, null);
	}

	private int getBtcPrecision() {
		final String precision = prefs.getString(PREFS_KEY_BTC_PRECISION, null);
		if (precision != null)
			return precision.charAt(0) - '0';
		else
			return PREFS_DEFAULT_BTC_PRECISION;
	}

	public boolean isNotificationsEnabled() {
		return prefs.getBoolean(PREFS_KEY_NOTIF_TRANSACTIONS, false);
	}

	public int getBtcShift() {
		final String precision = prefs.getString(PREFS_KEY_BTC_PRECISION, null);
		if (precision != null)
			return precision.length() == 3 ? precision.charAt(2) - '0' : 0;
		else
			return PREFS_DEFAULT_BTC_SHIFT;
	}

	public MonetaryFormat getFormat() {
		final int shift = getBtcShift();
		final int minPrecision = shift <= 3 ? 2 : 0;
		final int decimalRepetitions = (getBtcPrecision() - minPrecision) / 2;
		return new MonetaryFormat().shift(shift).minDecimals(minPrecision).repeatOptionalDecimals(2, decimalRepetitions);
	}


	public String getTrustedPeerHost() {
		return prefs.getString(PREFS_KEY_TRUSTED_PEER, "").trim();
	}

	public boolean getTrustedPeerOnly()	{
		return prefs.getBoolean(PREFS_KEY_TRUSTED_PEER_ONLY, false);
	}


	public void armBackupReminder()	{
		prefs.edit().putBoolean(PREFS_KEY_REMIND_BACKUP, true).apply();
	}

	public String getExchangeCurrencyCode()	{
		return prefs.getString(PREFS_KEY_EXCHANGE_CURRENCY, null);
	}

	public long getLastUsedAgo() {
		return prefs.getLong(PREFS_KEY_LAST_USED, 0);
	}

	public void touchLastUsed()	{
		final long prefsLastUsed = prefs.getLong(PREFS_KEY_LAST_USED, 0);
		final long now = System.currentTimeMillis();
		prefs.edit().putLong(PREFS_KEY_LAST_USED, now).apply();

		log.info("just being used - last used {} minutes ago", (now - prefsLastUsed) / DateUtils.MINUTE_IN_MILLIS);
	}

	public int getBestChainHeightEver()	{
		return prefs.getInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, 0);
	}

	public void maybeIncrementBestChainHeightEver(final int bestChainHeightEver) {
		if (bestChainHeightEver > getBestChainHeightEver())
			prefs.edit().putInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, bestChainHeightEver).apply();
	}

	public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		prefs.registerOnSharedPreferenceChangeListener(listener);
	}

	public void unregisterOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener)	{
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
	}

	public File getMainWalletFile() {
		return new File(prefs.getString(PREFS_KEY_MAIN_WALLET_FILE, Constants.WALLET.FIRST_WALLET_FILE.getAbsolutePath()));
	}

	public void setMainWalletFile(File f) {
		if (!f.exists()) {
			Log.e(TAG, "File " + f.getAbsolutePath() + " not exist.");
		} else {
			Editor edit = prefs.edit();
			edit.putString(PREFS_KEY_MAIN_WALLET_FILE, f.getAbsolutePath());
			edit.apply();
		}
	}

	public int getHDAccounts() {
		return prefs.getInt(PREFS_KEY_HD_ACCOUNTS, 1);
	}

	public void setHDAccounts(int accounts) {
		if (accounts > Constants.WALLET.MAX_HD_ACCOUNTS) {
			throw new IllegalArgumentException("The number of accounts hd may not be top 5");
		}

		Editor editor = prefs.edit();
		editor.putInt(PREFS_KEY_HD_ACCOUNTS, accounts);
		editor.apply();
	}

	public void setIdleDetectionEnabled(boolean detect) {
		prefs.edit().putBoolean(PREFS_KEY_IDLE_DETECTION, detect).apply();
	}

	public boolean isIdleDetectionEnabled() {
		return prefs.getBoolean(PREFS_KEY_IDLE_DETECTION, true);
	}

	public boolean isExchangeValueEnabled() {
		return prefs.getBoolean(PREFS_KEY_SHOW_EXCHANGE_VALUE, false);
	}

	public long getPriceUpdateInterval() {
        return Long.parseLong(prefs.getString(PREFS_KEY_PRICE_UPDATE_INTERVAL, "300000"));
    }

    public boolean isSpendPendintTxAvailable() {
		return prefs.getBoolean(PREFS_KEY_SPEND_UNCONFIRMED_TX, false);
	}

	public static Configuration getInstance() {
		return new Configuration(WalletApplication.INSTANCE);
	}
}
