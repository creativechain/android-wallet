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

package crea.wallet.lite.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.ui.main.BitcoinTransactionActivity;
import crea.wallet.lite.ui.main.MainActivity;
import crea.wallet.lite.util.TimeUtils;
import crea.wallet.lite.wallet.WalletHelper;
import crea.wallet.lite.wallet.WalletUtils;
import com.chip_chap.services.cash.coin.BitCoin;
import com.chip_chap.services.status.TransactionStatus;
import com.chip_chap.services.task.Task;
import com.chip_chap.services.transaction.Btc2BtcTransaction;

import org.creacoinj.core.Address;
import org.creacoinj.core.Block;
import org.creacoinj.core.BlockChain;
import org.creacoinj.core.CheckpointManager;
import org.creacoinj.core.Coin;
import org.creacoinj.core.FilteredBlock;
import org.creacoinj.core.Peer;
import org.creacoinj.core.PeerAddress;
import org.creacoinj.core.PeerGroup;
import org.creacoinj.core.Sha256Hash;
import org.creacoinj.core.StoredBlock;
import org.creacoinj.core.Transaction;
import org.creacoinj.core.TransactionBroadcast;
import org.creacoinj.core.TransactionConfidence.ConfidenceType;
import org.creacoinj.core.listeners.DownloadProgressTracker;
import org.creacoinj.core.listeners.PeerConnectedEventListener;
import org.creacoinj.core.listeners.PeerDisconnectedEventListener;
import org.creacoinj.net.discovery.DnsDiscovery;
import org.creacoinj.net.discovery.PeerDiscovery;
import org.creacoinj.net.discovery.PeerDiscoveryException;
import org.creacoinj.store.BlockStore;
import org.creacoinj.store.BlockStoreException;
import org.creacoinj.store.SPVBlockStore;
import org.creacoinj.utils.Threading;
import org.creacoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import static crea.wallet.lite.application.Constants.WALLET.CONTEXT;
import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;


/**
 * @author Andreas Schildbach
 */
public class BitcoinService extends PersistentService implements BlockchainService {

	private static final String TAG = "BitcoinService";
	private static final int NOTIFICATION_ID_CONNECTED = 0;
	private static final int NOTIFICATION_ID_COINS_RECEIVED = 1;
	private static final int NOTIFICATION_ID_BLOCKCHAIN_PROGRESS = 2;
	private static final int MIN_COLLECT_HISTORY = 2;
	private static final int IDLE_BLOCK_TIMEOUT_MIN = 2;
	private static final int IDLE_TRANSACTION_TIMEOUT_MIN = 9;
	private static final int MAX_HISTORY_SIZE = Math.max(IDLE_TRANSACTION_TIMEOUT_MIN, IDLE_BLOCK_TIMEOUT_MIN);

	private static final String CASH_IN_TITLE = "BTC Cash In";
	private static final String CASH_OUT_TITLE = "BTC Cash Out";

	public static TextView progressBar;

	private final AbstractWalletCoinListener WALLET_COIN_LISTENER = new AbstractWalletCoinListener() {
		@Override
		public void onCoinsReceived(Wallet wallet, final Transaction tx, Coin coin, Coin coin1) {
            org.creacoinj.core.Context.propagate(CONTEXT);
			Log.e(TAG,"Coins received!");
			transactionsReceived.incrementAndGet();

			final int bestChainHeight = blockChain.getBestChainHeight();

			final Address[] addresses = WalletUtils.getWalletAddressOfReceived(tx, wallet);
			final Coin amount = tx.getValue(wallet);
			final ConfidenceType confidenceType = tx.getConfidence().getConfidenceType();

			WalletHelper.INSTANCE.save();
			//TO VERIFY ONLY NEW INPUT TRANSACTIONS
			handler.post(new Runnable()	{
				@Override
				public void run() {
					final boolean isReceived = amount.signum() > 0;
					final boolean replaying = bestChainHeight < config.getBestChainHeightEver();
					final boolean isReplayedTx = confidenceType == ConfidenceType.BUILDING && replaying;

					if (isReceived && !isReplayedTx) {
						//NOTIFY INPUT TRANSACTION
						long id = saveTransaction(tx, CASH_IN_TITLE);
						Log.e(TAG,"Saved transaction, id : " + id);
						if (id > 0) {
							notifyCoinsReceived(amount, id, addresses);
							broadcastTransactionReceived(id);
						}
					}
				}
			});
		}

		@Override
		public void onCoinsSent(Wallet wallet, Transaction tx, Coin coin, Coin coin1) {
			transactionsReceived.incrementAndGet();
			long id = saveTransaction(tx, CASH_OUT_TITLE);
			broadcastTransactionSend(id);
		}
	};

	private final Set<BlockchainState.Impediment> impediments = EnumSet.noneOf(BlockchainState.Impediment.class);
	private final Handler handler = new Handler();
	private final Handler delayHandler = new Handler();
	private final List<Address> notificationAddresses = new LinkedList<Address>();

	private static Task<Transaction> onBroadcastTransaction;
	private WalletApplication application;
	private Configuration config;

	private BlockStore blockStore;
	private File blockChainFile;
	private BlockChain blockChain;


	private PeerGroup peerGroup;
	private WakeLock wakeLock;
	private PeerConnectivityListener peerConnectivityListener;
	private NotificationManager nm;
	private Coin notificationAccumulatedAmount = Coin.ZERO;
	private AtomicInteger transactionsReceived = new AtomicInteger();
	private int notificationCount = 0;
	private long serviceCreatedAt;
	private boolean resetBlockchainOnShutdown = false;
	private boolean isIdle = false;
	private boolean startService = false;

	public BitcoinService() {
		super("BitcoinService");
	}

	private long saveTransaction(Transaction tx, String title) {
		boolean cashIn = title.equals(CASH_IN_TITLE);
		if (!Btc2BtcTransaction.exist(tx.getHashAsString())) {
			final Btc2BtcTransaction transaction = new Btc2BtcTransaction();
			transaction.setOrigins(WalletUtils.getAddressStrings(tx, WalletUtils.TRANSACTION_INPUTS, cashIn));
			transaction.setDestiny(WalletUtils.getAddressStrings(tx, WalletUtils.TRANSACTION_OUTPUTS, cashIn));

			transaction.setConfirmations(0);
			transaction.setTitle(title);
			transaction.setTxHash(tx.getHashAsString());
			transaction.setStatus(TransactionStatus.OK);
			transaction.setDate(String.valueOf(System.currentTimeMillis()));

			if (cashIn) {
				transaction.setSatoshis(WalletHelper.INSTANCE.getValueSentToMe(tx).getValue());
			} else {
				transaction.setSatoshis(WalletHelper.INSTANCE.getValueSentFromMe(tx).getValue());
			}

			Coin fee = tx.getFee() != null ? tx.getFee() : Coin.ZERO;

			Log.e(TAG,tx.toString());

			transaction.setFee(fee.getValue());
			return transaction.save();
		}

		return 0;
	}

	private void broadcastTransactionSend(long id) {
		notifyTransaction(id, BlockchainBroadcastReceiver.TRANSACTION_SENT);
	}

	private void broadcastTransactionReceived(long id) {
		notifyTransaction(id, BlockchainBroadcastReceiver.TRANSACTION_RECEIVED);
	}

	private void notifyTransaction(long id, String action) {
		Intent sendIntetn = new Intent(action);
		sendIntetn.putExtra("txId", id);
		sendBroadcast(sendIntetn);
	}

	private void notifyCoinsReceived(final Coin amount, long id, @Nullable final Address... addresses) {
		if (!config.isNotificationsEnabled()) {
			return;
		}

		if (notificationCount == 1) {
			nm.cancel(NOTIFICATION_ID_COINS_RECEIVED);
		}

		notificationCount++;
		notificationAccumulatedAmount = notificationAccumulatedAmount.add(amount);

		String person = TextUtils.join(",", WalletUtils.getAddressStrings(addresses));

		BitCoin bitCoin = BitCoin.valueOf(notificationAccumulatedAmount.getValue());
		String msg;
		Intent intent;

		if (notificationCount < 2) {
			msg = getString(R.string.notif_cash_in, bitCoin.toFriendlyString(), person);
			intent = new Intent(this, BitcoinTransactionActivity.class);
		} else {
			msg = getString(R.string.notif_accumulated_amount, bitCoin.toFriendlyString(), notificationCount);
			intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		}

		intent.putExtra("id", id);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		String title = getString(R.string.notif_cash_in_title, bitCoin.toFriendlyString());
		final NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
		notification.setSmallIcon(R.mipmap.ic_launcher);
		notification.setContentText(msg);
		notification.setContentTitle(title);
		notification.setWhen(System.currentTimeMillis());
		notification.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		notification.setContentIntent(pendingIntent);
		Notification notif = notification.build();
		notif.flags |= Notification.FLAG_AUTO_CANCEL;
		nm.notify(NOTIFICATION_ID_COINS_RECEIVED, notif);
	}

	private final class PeerConnectivityListener implements PeerConnectedEventListener, PeerDisconnectedEventListener, OnSharedPreferenceChangeListener {

		private AtomicBoolean stopped = new AtomicBoolean(false);

		public PeerConnectivityListener(){
			config.registerOnSharedPreferenceChangeListener(this);
		}

		public void stop() {
			stopped.set(true);
			config.unregisterOnSharedPreferenceChangeListener(this);

			//REMOVE NOTIFICATIONS?
			nm.cancel(NOTIFICATION_ID_CONNECTED);
		}

		@Override
		public void onPeerConnected(final Peer peer, final int peerCount) {
			Log.i(TAG, "Connected " + peer.getAddress().toString());
		}

		@Override
		public void onPeerDisconnected(final Peer peer, final int peerCount) {
			Log.i(TAG, "Disconnected " + peer.getAddress().toString());
		}

		@Override
		public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
		}


	}

	private final DownloadProgressTracker blockchainDownloadListener = new DownloadProgressTracker() {
		private final long CALLBACK_TIME = 1000L;
		private final AtomicLong lastMessageTime = new AtomicLong(0);

		private long bestHeight;
		private int height;
		private int remain;
		private String remainingTime = "";

        private int downloaded = 0;
        private int time = 0;
		private int blockSpeed = 0;
        private boolean rateStarted = false;
        private Handler rateHandler = new Handler();

		@Override
		public void onBlocksDownloaded(final Peer peer, final Block block, final FilteredBlock filteredBlock, final int blocksLeft) {
            this.bestHeight = peer.getBestHeight();
			this.height = (int) bestHeight - blocksLeft;
			this.remain = blocksLeft;
			blockSpeed++;

			delayHandler.removeCallbacksAndMessages(null);

			final long now = System.currentTimeMillis();

			if (now - lastMessageTime.get() > CALLBACK_TIME) {
				delayHandler.post(RUNNER);
			} else {
				delayHandler.postDelayed(RUNNER, CALLBACK_TIME);
			}

            if (!rateStarted) {
                rateStarted = true;
                rateHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        time++;
                        downloaded = downloaded + blockSpeed;
                        double average = downloaded / time;
                        if (average > 0) {
                            long remainTime = Math.round(remain / average * 1000L);
                            remainingTime = TimeUtils.toHumanReadable(remainTime);
                        }

                        Log.i(TAG, remainingTime + ", REMAINS=" + remain + ", " + blockSpeed + " blocks/s");
                        blockSpeed = 0;
                        rateHandler.postDelayed(this, 1000);
                    }
                });
            }

			if (blocksLeft == 0) {
                Coin estimated = WalletHelper.INSTANCE.getTotalBalance(Wallet.BalanceType.ESTIMATED);
                Coin available = WalletHelper.INSTANCE.getTotalBalance(Wallet.BalanceType.AVAILABLE);
                Coin pending = estimated.minus(available);

                Log.d(TAG, "ESTIMATED = " + estimated.toFriendlyString());
                Log.d(TAG, "AVAILABLE = " + available.toFriendlyString());
                Log.d(TAG, "PENDING = " + pending.toFriendlyString());
                //WalletHelper.INSTANCE.cleanup();
				sendBroadcast(new Intent(BlockchainBroadcastReceiver.LAST_BLOCK_RECEIVED));
                rateHandler.removeCallbacksAndMessages(null);
				if (progressBar != null) {
					progressBar.post(new Runnable() {
						@Override
						public void run() {
							progressBar.setText(getString(R.string.amount_in_currency, config.getMainCurrency().getName()));
						}
					});
				}
			}

		}

		private final Runnable RUNNER = new Runnable() {
			@Override
			public void run() {
                lastMessageTime.set(System.currentTimeMillis());
				//WalletHelper.INSTANCE.setLastBlock(block, height);

				if (progressBar != null) {
					progressBar.post(new Runnable() {
						@Override
						public void run() {
							progressBar.setText(getString(R.string.sync_time, remainingTime));
						}
					});
				}

				config.maybeIncrementBestChainHeightEver(blockChain.getChainHead().getHeight());
			}
		};
	};

	private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			Log.e(TAG,"Context: " + context.getClass().getName() + ", action: " + action);
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))	{
				final boolean hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				Log.i(TAG, "network is " + (hasConnectivity ? "up" : "down"));

				if (hasConnectivity) {
					impediments.remove(BlockchainState.Impediment.NETWORK);
				} else {
					impediments.add(BlockchainState.Impediment.NETWORK);
				}

				check();
			} else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action))	{
				Log.i(TAG, "device storage low");
				impediments.add(BlockchainState.Impediment.STORAGE);
				check();
			} else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action))	{
				Log.i(TAG, "device storage ok");
				impediments.remove(BlockchainState.Impediment.STORAGE);
				check();
			}
		}

		@SuppressLint("Wakelock")
		private void check() {

			if (impediments.isEmpty() && peerGroup == null)	{
				Log.d(TAG, "acquiring wakelock");
				wakeLock.acquire();

				// consistency check
				final int walletLastBlockSeenHeight = WalletHelper.INSTANCE.getLastBlockHeight();
				final int bestChainHeight = blockChain.getBestChainHeight();
				if (walletLastBlockSeenHeight != -1 && walletLastBlockSeenHeight != bestChainHeight) {
					final String message = "wallet/blockchain out of sync: " + walletLastBlockSeenHeight + "/" + bestChainHeight;
					Log.e(TAG,message);
				}

                org.creacoinj.core.Context.propagate(CONTEXT);

				Log.i(TAG, "starting peergroup");
				peerGroup = new PeerGroup(NETWORK_PARAMETERS, blockChain);
				peerGroup.setDownloadTxDependencies(0); // recursive implementation causes StackOverflowError
				peerGroup.setBloomFilteringEnabled(true);
				addWallets();

				peerGroup.setUserAgent(Constants.APP.CLIENT_NAME, Constants.APP.VERSION);
				peerGroup.addConnectedEventListener(peerConnectivityListener);
				peerGroup.addDisconnectedEventListener(peerConnectivityListener);

				final int maxConnectedPeers = application.maxConnectedPeers();

				final String trustedPeerHost = config.getTrustedPeerHost();
				final boolean hasTrustedPeer = !trustedPeerHost.isEmpty();

				final boolean connectTrustedPeerOnly = hasTrustedPeer && config.getTrustedPeerOnly();
				peerGroup.setMaxConnections(connectTrustedPeerOnly ? 1 : maxConnectedPeers);
				peerGroup.setConnectTimeoutMillis(Constants.PEER_TIMEOUT_MS);
				peerGroup.setPeerDiscoveryTimeoutMillis(Constants.PEER_DISCOVERY_TIMEOUT_MS);

				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						String[] peers = {"80.241.212.178"};

						for (int x = 0; x < peers.length; x++) {
							try {
								peerGroup.addAddress(new PeerAddress(NETWORK_PARAMETERS, InetAddress.getByName(peers[x]), NETWORK_PARAMETERS.getPort()));
							} catch (UnknownHostException e) {
								e.printStackTrace();
							}
						}
						return null;
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


/*				peerGroup.addPeerDiscovery(new PeerDiscovery() {
					private final PeerDiscovery normalPeerDiscovery = new DnsDiscovery(Constants.WALLET.NETWORK_PARAMETERS);

					@Override
					public InetSocketAddress[] getPeers(long services, final long timeoutValue, final TimeUnit timeoutUnit) throws PeerDiscoveryException {
						final List<InetSocketAddress> peers = new LinkedList<InetSocketAddress>();

						boolean needsTrimPeersWorkaround = false;

						if (hasTrustedPeer) {
							Log.i(TAG, "trusted peer '" + trustedPeerHost + "'" + (connectTrustedPeerOnly ? " only" : ""));

							final InetSocketAddress addr = new InetSocketAddress(trustedPeerHost, Constants.WALLET.NETWORK_PARAMETERS.getPort());
							if (addr.getAddress() != null) {
								peers.add(addr);
								needsTrimPeersWorkaround = true;
							}
						}

						if (!connectTrustedPeerOnly) {
							peers.addAll(Arrays.asList(normalPeerDiscovery.getPeers(services, timeoutValue, timeoutUnit)));
						}

						// workaround because PeerGroup will shuffle peers
						if (needsTrimPeersWorkaround) {
							while (peers.size() >= maxConnectedPeers)
								peers.remove(peers.size() - 1);
						}

						InetSocketAddress[] isas = new InetSocketAddress[0];
						Log.e(TAG,"Peers: " + peers.toString());
						return peers.toArray(isas);
					}

					@Override
					public void shutdown() {
						normalPeerDiscovery.shutdown();
					}
				});*/

				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// start peergroup
						Log.e(TAG,"Starting blockchain download...");
						peerGroup.startAsync();
						peerGroup.startBlockChainDownload(blockchainDownloadListener);
						return null;
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			} else if (!impediments.isEmpty() && peerGroup != null)	{
				Log.i(TAG, "stopping peergroup");
				peerGroup.removeDisconnectedEventListener(peerConnectivityListener);
				peerGroup.removeConnectedEventListener(peerConnectivityListener);
				removeWallets();
				peerGroup.stopAsync();
				peerGroup = null;

				Log.d(TAG, "releasing wakelock");
				wakeLock.release();
			}
		}
	};

	private final static class ActivityHistoryEntry	{
		public final int numTransactionsReceived;
		public final int numBlocksDownloaded;

		public ActivityHistoryEntry(final int numTransactionsReceived, final int numBlocksDownloaded) {
			this.numTransactionsReceived = numTransactionsReceived;
			this.numBlocksDownloaded = numBlocksDownloaded;
		}

		@Override
		public String toString()
		{
			return numTransactionsReceived + "/" + numBlocksDownloaded;
		}
	}

	private final BroadcastReceiver tickReceiver = new BroadcastReceiver()	{
		private int lastChainHeight = 0;
		private final List<ActivityHistoryEntry> activityHistory = new LinkedList<ActivityHistoryEntry>();

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int chainHeight = blockChain.getBestChainHeight();

			if (lastChainHeight > 0) {
				final int numBlocksDownloaded = chainHeight - lastChainHeight;
				final int numTransactionsReceived = transactionsReceived.getAndSet(0);

				// push history
				activityHistory.add(0, new ActivityHistoryEntry(numTransactionsReceived, numBlocksDownloaded));

				// trim
				while (activityHistory.size() > MAX_HISTORY_SIZE) {
					activityHistory.remove(activityHistory.size() - 1);
				}

				// print
				final StringBuilder builder = new StringBuilder();
				for (final ActivityHistoryEntry entry : activityHistory){
					if (builder.length() > 0) {
						builder.append(", ");
					}
					builder.append(entry);
				}
				Log.i(TAG, "History of transactions/blocks: " + builder);

				// determine if block and transaction activity is idling
				isIdle = false;
				if (activityHistory.size() >= MIN_COLLECT_HISTORY) {
					isIdle = true;
					for (int i = 0; i < activityHistory.size(); i++) {
						final ActivityHistoryEntry entry = activityHistory.get(i);
						final boolean blocksActive = entry.numBlocksDownloaded > 0 && i <= IDLE_BLOCK_TIMEOUT_MIN;
						final boolean transactionsActive = entry.numTransactionsReceived > 0 && i <= IDLE_TRANSACTION_TIMEOUT_MIN;

						if (blocksActive || transactionsActive)	{
							isIdle = false;
							break;
						}
					}
				}

				// if idling, shutdown service
				if (isIdle)	{
					Log.i(TAG, "idling detected");
					/*stopSelf();*/
				}
			}

			lastChainHeight = chainHeight;
		}
	};

	private void addWallets() {
		if (peerGroup != null) {
			WalletHelper.INSTANCE.addIssuedAddressesToWatch();
			WalletHelper.INSTANCE.addEventListener(WALLET_COIN_LISTENER, Threading.THREAD_POOL);
			int count =0;
			for (Wallet w : WalletHelper.INSTANCE.getWallets()) {
				peerGroup.addWallet(w);

				count++;
			}

			Log.e(TAG,"Watching " + count + " wallets");
		}
	}

	private void removeWallets() {
		if (peerGroup != null) {
			WalletHelper.INSTANCE.removeEventListener(WALLET_COIN_LISTENER);
			int count =0;
			for (Wallet w : WalletHelper.INSTANCE.getWallets()) {
				peerGroup.removeWallet(w);
				count++;
			}

			Log.e(TAG,"Removed " + count + " wallets");
		}
	}

	public class LocalBinder extends Binder {
		public BlockchainService getService()
		{
			return BitcoinService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(final Intent intent)	{
		Log.e(TAG,".onBind()");
		return mBinder;
	}

	@Override
	public void onCreate()	{
		serviceCreatedAt = System.currentTimeMillis();
		super.onCreate();

		startService = WalletHelper.INSTANCE != null;

		Log.e(TAG,"START_SERVICE=" + startService);
		if (startService) {
			nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            WalletHelper.INSTANCE.autoSave(10);
			final String lockName = getPackageName() + " blockchain sync";

			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);
			application = WalletApplication.INSTANCE;
			config = Configuration.getInstance();

			setWalletListener();

			peerConnectivityListener = new PeerConnectivityListener();

			blockChainFile = Constants.WALLET.BLOCKCHAIN_FILE;
			final boolean blockChainFileExists = blockChainFile.exists();

			if (!blockChainFileExists) {
				new File(Constants.WALLET.WALLET_PATH).mkdirs();
			}
			try	{
				blockStore = new SPVBlockStore(NETWORK_PARAMETERS, blockChainFile);
				blockStore.getChainHead(); // detect corruptions as early as possible

				final long earliestKeyCreationTime = WalletHelper.INSTANCE.getFirstKeyCreationTime();

				Log.e(TAG,"CREATION_TIME=" + earliestKeyCreationTime +  " BLOCKCHAIN_FILE=" + blockChainFileExists);
/*				if (!blockChainFileExists && earliestKeyCreationTime > 0){
					try	{
						config.setDeletingBlockchain(true);
						final long start = System.currentTimeMillis();
						final InputStream checkpointsInputStream = getAssets().open("bitcoin/" + Constants.WALLET.CHECKPOINTS_FILENAME);
						CheckpointManager.checkpoint(NETWORK_PARAMETERS, checkpointsInputStream, blockStore, earliestKeyCreationTime);
						Log.e(TAG,String.format("checkpoints loaded from '%1$s', took %2$dms", Constants.WALLET.CHECKPOINTS_FILENAME, System.currentTimeMillis() - start));
					} catch (final IOException x) {
						Log.e(TAG, "problem reading checkpoints, continuing without", x);
					}
				}*/
			} catch (final BlockStoreException x) {
				blockChainFile.delete();

				final String msg = "blockstore cannot be created";
				Log.e(TAG,msg, x);
			}

			try	{
				blockChain = new BlockChain(NETWORK_PARAMETERS, WalletHelper.INSTANCE.getMainWallet(), blockStore);
			} catch (final BlockStoreException x) {
				throw new Error("blockchain cannot be created", x);
			}


			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
			intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
			registerReceiver(connectivityReceiver, intentFilter); // implicitly start PeerGroup

			registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		} else {
			Log.e(TAG, "Incomplete conditions for start service");
			stopSelf();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.e(TAG, "onHandleIntent");

        org.creacoinj.core.Context.propagate(CONTEXT);
		if (intent != null)	{
			Log.i(TAG, "service start command: " + intent
					+ (intent.hasExtra(Intent.EXTRA_ALARM_COUNT) ? " (alarm count: " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0) + ")" : ""));

			final String action = intent.getAction();

			if (ACTION_CANCEL_COINS_RECEIVED.equals(action)) {
				notificationCount = 0;
				notificationAccumulatedAmount = Coin.ZERO;
				notificationAddresses.clear();

				if (nm != null) {
					nm.cancel(NOTIFICATION_ID_COINS_RECEIVED);
				}

			} else if (ACTION_BROADCAST_TRANSACTION.equals(action)) {

				final Sha256Hash hash = new Sha256Hash(intent.getByteArrayExtra(ACTION_BROADCAST_TRANSACTION_HASH));
				final Transaction tx = WalletHelper.INSTANCE.getTransaction(hash);

				if (peerGroup != null) {
					Log.i(TAG, "broadcasting transaction " + hash.toString());
					onBroadcastTransaction = new Task<Transaction>() {
						@Override
						public void doTask(Transaction transaction) {
							long id = saveTransaction(transaction, CASH_OUT_TITLE);
							broadcastTransactionSent(id);
						}
					};

					TransactionBroadcast tb = peerGroup.broadcastTransaction(tx);
					tb.future().addListener(new Runnable() {
						@Override
						public void run() {
							onBroadcastTransaction.doTask(tx);
						}
					}, new Executor() {
						@Override
						public void execute(@NonNull Runnable command) {
							command.run();
						}
					});

				} else {
					Log.i(TAG, "PeerGroup not available, not broadcasting transaction " + hash.toString());
				}
			} else if (ACTION_RESET_BLOCKCHAIN.equals(action)) {
				Log.i(TAG, "will remove blockchain on service shutdown");

				resetBlockchainOnShutdown = true;
				stopSelf();
			}
		} else {
			Log.e(TAG,"service restart, although it was started as non-sticky");
		}
	}

	@Override
	public void onDestroy()	{
		Log.d(TAG, ".onDestroy()");

		if(startService) {
			WalletApplication.scheduleStartBlockchainService(this);
			unregisterReceiver(tickReceiver);

			WalletHelper.INSTANCE.removeEventListener(WALLET_COIN_LISTENER);
			unregisterReceiver(connectivityReceiver);

			if (peerGroup != null)	{
				peerGroup.removeDisconnectedEventListener(peerConnectivityListener);
				peerGroup.removeConnectedEventListener(peerConnectivityListener);
				peerGroup.removeWallet(WalletHelper.INSTANCE.getMainWallet());
				peerGroup.stop();

				Log.i(TAG, "peergroup stopped");
			}

			peerConnectivityListener.stop();
			delayHandler.removeCallbacksAndMessages(null);

			try	{
				blockStore.close();
			} catch (final BlockStoreException x) {
				throw new RuntimeException(x);
			}

			removeWalletListener();
			WalletHelper.INSTANCE.save();

			if (wakeLock.isHeld())	{
				Log.d(TAG, "wakelock still held, releasing");
				wakeLock.release();
			}

			if (resetBlockchainOnShutdown) {
				Log.i(TAG, "removing blockchain");
				blockChainFile.delete();
                WalletHelper.INSTANCE.reset();
                broadCastBlockChainReset();
			}

			if (isIdle) {
				WalletApplication.INSTANCE.stopBlockchainService();
			}
			super.onDestroy();
		}


		Log.i(TAG, "service was up for " + ((System.currentTimeMillis() - serviceCreatedAt) / 1000 / 60) + " minutes");
	}

	@Override
	public void onTrimMemory(final int level) {
		Log.i(TAG, String.format("onTrimMemory(%1$d) called", level));

		if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
			Log.d(TAG, "low memory detected, stopping service");
			stopSelf();
		}
	}

	@Override
	public BlockchainState getBlockchainState()	{
		final StoredBlock chainHead = blockChain.getChainHead();
		final Date bestChainDate = chainHead.getHeader().getTime();
		final int bestChainHeight = chainHead.getHeight();
		final boolean replaying = chainHead.getHeight() < config.getBestChainHeightEver();

		return new BlockchainState(bestChainDate, bestChainHeight, replaying, impediments);
	}

	@Override
	public List<Peer> getConnectedPeers() {
		if (peerGroup != null) {
			return peerGroup.getConnectedPeers();
		} else {
			return null;
		}
	}

	@Override
	public List<StoredBlock> getRecentBlocks(final int maxBlocks) {
		final List<StoredBlock> blocks = new ArrayList<StoredBlock>(maxBlocks);

		try	{
			StoredBlock block = blockChain.getChainHead();

			while (block != null) {
				blocks.add(block);

				if (blocks.size() >= maxBlocks) {
					break;
				}

				block = block.getPrev(blockStore);
			}
		} catch (final BlockStoreException x) {
			// swallow
		}

		return blocks;
	}

	public void broadcastTransactionSent(long txId) {
		Intent i = new Intent(BlockchainBroadcastReceiver.TRANSACTION_SENT);
        i.putExtra("txId", txId);
		sendBroadcast(i);
	}

    public void broadCastBlockChainReset() {
        Intent i = new Intent(BlockchainBroadcastReceiver.BLOCKCHAIN_RESET);
        sendBroadcast(i);
    }

	private void setWalletListener() {
		WalletHelper.INSTANCE.addEventListener(WALLET_COIN_LISTENER, Threading.SAME_THREAD);
	}

	private void removeWalletListener() {
		WalletHelper.INSTANCE.removeEventListener(WALLET_COIN_LISTENER);
	}
}
