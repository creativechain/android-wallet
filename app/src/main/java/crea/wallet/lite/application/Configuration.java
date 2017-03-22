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
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.status.TransactionStatus;

import org.creacoinj.core.Coin;
import org.creacoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

/**
 * @author Andreas Schildbach
 */
public class Configuration {

	private static final String TAG = "Configuration";
	private final SharedPreferences prefs;
	private final Resources res;

	public static final String PREFS_KEY_BTC_PRECISION = "btc_precision";
	public static final String PREFS_KEY_CONNECTIVITY_NOTIFICATION = "connectivity_notification";
	public static final String PREFS_KEY_EXCHANGE_CURRENCY = "exchange_currency";
	public static final String PREFS_KEY_TRUSTED_PEER = "trusted_peer";
	public static final String PREFS_KEY_TRUSTED_PEER_ONLY = "trusted_peer_only";
	public static final String PREFS_KEY_REMIND_BACKUP = "remind_backup";
	public static final String PREFS_KEY_LAST_PRICE = "last_price";
	public static final String PREFS_KEY_TRANSACTION_FEE = "transaction_fee";
	public static final String PREFS_KEY_NOTIF_TRANSACTIONS = "notif_transactions";
	public static final String PREFS_KEY_STATUS_LIST = "list_status";
	public static final String PREFS_KEY_FAVOURITE_CASH_METHODS = "favourite_cash_methods";
	public static final String PREFS_KEY_FIRST_USE_HISTORY = "isFirstUse";
	public static final String PREFS_KEY_FIRST_USE_MAIN = "first_use_main";
	public static final String PREFS_KEY_RESTART_TIPS = "restart_tips";
	public static final String PREFS_KEY_AUTOCLEAR_LIST = "autoClearList";
	public static final String PREFS_KEY_DELETE_BLOCKCHAIN = "delete_blockchain";
	public static final String PREFS_KEY_MAIN_WALLET_FILE = "main_wallet_file";
	public static final String PREFS_KEY_HD_ACCOUNTS = "hd_accounts";
	public static final String PREFS_KEY_MAIN_CURRENCY = "main_currency";
	public static final String PREFS_KEY_BTC_PRICE_EUR = "btc_price_eur";
	public static final String PREFS_KEY_BTC_PRICE_PLN = "btc_price_pln";
	public static final String PREFS_KEY_BTC_PRICE_MXN = "btc_price_mxn";
	public static final String PREFS_KEY_BTC_PRICE_USD = "btc_price_usd";

	private static final int PREFS_DEFAULT_BTC_SHIFT = 8;
	private static final int PREFS_DEFAULT_BTC_PRECISION = 8;
	private static final String PREFS_KEY_LAST_USED = "last_used";
	private static final String PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever";
	private static final String PREFS_KEY_CACHED_EXCHANGE_CURRENCY = "cached_exchange_currency";
	private static final String PREFS_KEY_CACHED_EXCHANGE_RATE_COIN = "cached_exchange_rate_coin";
	private static final String PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT = "cached_exchange_rate_fiat";
	private static final String PREFS_KEY_PASSCODE = "PASSCODE";

	private static final Logger log = LoggerFactory.getLogger(Configuration.class);

	public Configuration(Context context) {
		this(PreferenceManager.getDefaultSharedPreferences(context), context.getResources());
	}

	public Configuration(final SharedPreferences prefs, final Resources res) {
		this.prefs = prefs;
		this.res = res;
	}

	public com.chip_chap.services.cash.coin.base.Coin getBtcPrice(Currency c) {
		String key;
		Currency curr = c;
		switch (c) {
			case USD:
				key = PREFS_KEY_BTC_PRICE_USD;
				break;
			case PLN:
				key = PREFS_KEY_BTC_PRICE_PLN;
				break;
			case MXN:
				key = PREFS_KEY_BTC_PRICE_MXN;
				break;
			default:
				key = PREFS_KEY_BTC_PRICE_EUR;
				curr = Currency.EUR;
				break;
		}
		long val = prefs.getLong(key, 0);

		if (val >= 1) {
			return com.chip_chap.services.cash.coin.base.Coin.fromCurrency(curr, 1 / (val / 1e8d));
		} else {
			return com.chip_chap.services.cash.coin.base.Coin.fromCurrency(curr, 0);
		}

	}

	public com.chip_chap.services.cash.coin.base.Coin getPriceForMainCurrency() {
		return getBtcPrice(getMainCurrency());
	}

	public void setBtcPrice(Currency c, long value) {
		String key = null;
		switch (c) {
			case USD:
				key = PREFS_KEY_BTC_PRICE_USD;
				break;
			case PLN:
				key = PREFS_KEY_BTC_PRICE_PLN;
				break;
			case MXN:
				key = PREFS_KEY_BTC_PRICE_MXN;
				break;
			case EUR:
				key = PREFS_KEY_BTC_PRICE_EUR;
				break;
		}

		if (key != null) {
			Editor editor = prefs.edit();
			editor.putLong(key, value);
			editor.apply();
		}
	}

	public Currency getMainCurrency() {
		String curr = prefs.getString(PREFS_KEY_MAIN_CURRENCY, "EUR");
		return Currency.getCurrency(curr);
	}

	public void setMainCurrency(Currency c) {
		Editor editor = prefs.edit();
		editor.putString(PREFS_KEY_MAIN_CURRENCY, c.getCode());
		editor.apply();
	}

	public Coin getTransactionFee() {
		long feeValue = Long.parseLong(prefs.getString(PREFS_KEY_TRANSACTION_FEE, "95000"));
		return Coin.valueOf(feeValue);
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

	public boolean isFirstUseHistory() {
		return prefs.getBoolean(PREFS_KEY_FIRST_USE_HISTORY, true);
	}

	public boolean isAutoclearListEnabled() {
		return prefs.getBoolean(PREFS_KEY_AUTOCLEAR_LIST, false);
	}

	public boolean isRestartTips() {
		return prefs.getBoolean(PREFS_KEY_RESTART_TIPS, false);
	}

	public void setRestartTips(boolean restartTips) {
		Editor editor = prefs.edit();
		editor.putBoolean(PREFS_KEY_RESTART_TIPS, restartTips);
		editor.apply();
	}

	public void setFirstUseHistory(boolean firstUse) {
		Editor editor = prefs.edit();
		editor.putBoolean(PREFS_KEY_FIRST_USE_HISTORY, firstUse);
		editor.apply();
	}

	public boolean isFirstUseMain() {
		return prefs.getBoolean(PREFS_KEY_FIRST_USE_MAIN, true);
	}

	public void setFirstUseMain(boolean firstUse) {
		Editor editor = prefs.edit();
		editor.putBoolean(PREFS_KEY_FIRST_USE_MAIN, firstUse);
		editor.apply();
	}

	public TransactionStatus[] getStatusToNotify() {
		Set<String> statusSet = prefs.getStringSet(PREFS_KEY_STATUS_LIST, null);
		TransactionStatus[] statuses = new TransactionStatus[0];
		if (statusSet != null) {
			String[] all = statusSet.toArray(new String[statusSet.size()]);
			statuses = TransactionStatus.toArray(all);
		}

		return statuses;
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
		final long now = System.currentTimeMillis();

		return now - prefs.getLong(PREFS_KEY_LAST_USED, 0);
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

	public static Configuration getInstance() {
		return new Configuration(WalletApplication.INSTANCE);
	}
}
