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

package crea.wallet.lite.application;

import android.os.Environment;
import android.text.format.DateUtils;

import crea.wallet.lite.BuildConfig;
import com.google.common.io.BaseEncoding;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.Context;
import org.creativecoinj.core.NetworkParameters;
import org.creativecoinj.params.MainNetParams;
import org.creativecoinj.params.TestNet3Params;

import java.io.File;

/**
 * @author Andreas Schildbach
 */
public final class Constants {
	public static final boolean TEST = BuildConfig.DEBUG;

	/** Network this wallet is on (e.g. testnet or mainnet). */
	public final static class APP {

		public static final String VERSION = BuildConfig.VERSION_NAME;
		public static final String CREATIVECHAIN_PATH = FILES.DATA_DIRECTORY.getAbsolutePath() + "/Creativechain/";

		/** Folder for backups */
		public static final String BACKUP_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CreativeChain/Backup/";

		/** User-agent to use for network access. */
		public static final String CLIENT_NAME = "CreativecoinWallet";
	}

	public final static class WALLET {
		public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();
		public static final Context CONTEXT = new Context(NETWORK_PARAMETERS);
		public static final long MIN_CREATION_TIME = 1493683200000L;

		public static final String WALLET_PATH = APP.CREATIVECHAIN_PATH + "Wallet/";
		public static final String WALLET_FILES_NAME = WALLET_PATH + "wallet";
		public static final String WALLET_BACKUP_FILE_NAME = APP.BACKUP_FOLDER + "backup-protobuf";
		public static final File FIRST_WALLET_FILE = new File(WALLET_FILES_NAME + FILES.FILENAME_NETWORK_SUFFIX);

		/** Filename of the automatic wallet backup. */
		public static final File WALLET_BACKUP_FILE = new File(WALLET_BACKUP_FILE_NAME + FILES.FILENAME_NETWORK_SUFFIX);

		/** Filename of the block store for storing the chain. */
		public static final String BLOCKCHAIN_FILENAME = "chain" + FILES.FILENAME_NETWORK_SUFFIX;
		public static final File BLOCKCHAIN_FILE = new File(WALLET_PATH + BLOCKCHAIN_FILENAME);

		/** Filename of the block checkpoints file. */
		public static final String CHECKPOINTS_FILENAME = "checkpoints" + FILES.FILENAME_NETWORK_SUFFIX + "chkp";

		public static final String MAIN_ADDRESS = "CPw1tJF6wWcdXPKYwS4pg3wF37XVqJZ9JC";
		public static final String TEST_ADDRESS = "cLGBL8Jb5iAchT86xGrZjqHSRry1aya6bb";
		public static final String ADDRESS = TEST ? TEST_ADDRESS : MAIN_ADDRESS;
		public static final Address DONATION_ADDRESS = Address.fromBase58(WALLET.NETWORK_PARAMETERS, ADDRESS);

		public static final int SCRYPT_ITERATIONS_TARGET = 32768;
		public static final int MAX_HD_ACCOUNTS = 5;
	}

	public static final class URLS {
		private static final String BITEASY_API_URL_PROD = "https://api.biteasy.com/v2/btc/mainnet/";
		private static final String BITEASY_API_URL_TEST = "https://api.biteasy.com/v2/btc/testnet/";

		/** Base URL for blockchain API. */
		public static final String BITEASY_API_URL = TEST ? BITEASY_API_URL_TEST : BITEASY_API_URL_PROD;

		public static final String BLOCKEXPLORER_PROD_URL = "https://search.creativechain.net/api/getrawtransaction?txid=";
		public static final String BLOCKEXPLORER_TEST_URL = "https://testnet.blockexplorer.com/";
		public static final String BLOCKEXPLORER_URL = "https://search.creativechain.net/tx/";

		public static final String FEES_URL = "https://wallet.schildbach.de/fees";
	}

	public final static class FILES {
		public static final String FILENAME_NETWORK_SUFFIX = TEST ? ".cct" : ".ccm";

		/** Path to external storage */
		public static final File EXTERNAL_STORAGE_DIR = Environment.getExternalStorageDirectory();

		public static final String FEES_FILENAME = "fees" + FILENAME_NETWORK_SUFFIX;

		public static final File DATA_DIRECTORY = WalletApplication.INSTANCE.getFilesDir();
	}

	/** Maximum size of backups. Files larger will be rejected. */
	public static final long BACKUP_MAX_CHARS = 10000000;
	public static final char CHAR_THIN_SPACE = '\u2009';
	public static final char CURRENCY_PLUS_SIGN = '\uff0b';
	public static final char CURRENCY_MINUS_SIGN = '\uff0d';

	public static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

	public static final int HTTP_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;
	public static final int PEER_DISCOVERY_TIMEOUT_MS = 10 * (int) DateUtils.SECOND_IN_MILLIS;
	public static final int PEER_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;
	public static final long LAST_USAGE_THRESHOLD_JUST_MS = DateUtils.HOUR_IN_MILLIS;
	public static final long LAST_USAGE_THRESHOLD_RECENTLY_MS = 2 * DateUtils.DAY_IN_MILLIS;
	public static final int SDK_LOLLIPOP = 21;
	public static final int MEMORY_CLASS_LOWEND = 48;
}
