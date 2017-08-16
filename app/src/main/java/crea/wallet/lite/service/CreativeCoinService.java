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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import crea.wallet.lite.R;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.application.WalletApplication;
import crea.wallet.lite.broadcast.BlockchainBroadcastReceiver;
import crea.wallet.lite.ui.base.TransactionActivity;
import crea.wallet.lite.ui.main.CoinTransactionActivity;
import crea.wallet.lite.ui.main.MainActivity;
import crea.wallet.lite.util.TimeUtils;
import crea.wallet.lite.wallet.WalletHelper;
import crea.wallet.lite.wallet.WalletUtils;

import org.creativecoinj.core.Address;
import org.creativecoinj.core.Block;
import org.creativecoinj.core.BlockChain;
import org.creativecoinj.core.CheckpointManager;
import org.creativecoinj.core.Coin;
import org.creativecoinj.core.FilteredBlock;
import org.creativecoinj.core.Peer;
import org.creativecoinj.core.PeerGroup;
import org.creativecoinj.core.Sha256Hash;
import org.creativecoinj.core.StoredBlock;
import org.creativecoinj.core.Transaction;
import org.creativecoinj.core.TransactionConfidence.ConfidenceType;
import org.creativecoinj.core.listeners.DownloadProgressTracker;
import org.creativecoinj.core.listeners.PeerConnectedEventListener;
import org.creativecoinj.core.listeners.PeerDisconnectedEventListener;
import org.creativecoinj.net.discovery.SeedPeers;
import org.creativecoinj.store.BlockStore;
import org.creativecoinj.store.BlockStoreException;
import org.creativecoinj.store.SPVBlockStore;
import org.creativecoinj.utils.Threading;
import org.creativecoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import static crea.wallet.lite.application.Constants.WALLET.CONTEXT;
import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;
import static crea.wallet.lite.broadcast.BlockchainBroadcastReceiver.ACTION_SYNC_STARTED;


/**
 * @author Andreas Schildbach
 */
public class CreativeCoinService extends Service implements BlockchainService {

	private static final String TAG = "CreativeCoinService";
	private static final int NOTIFICATION_ID_CONNECTED = 0;
	private static final int NOTIFICATION_ID_COINS_RECEIVED = 1;
	private static final int NOTIFICATION_ID_BLOCKCHAIN_PROGRESS = 2;
	private static final int MIN_COLLECT_HISTORY = 2;
	private static final int IDLE_BLOCK_TIMEOUT_MIN = 2;
	private static final int IDLE_TRANSACTION_TIMEOUT_MIN = 9;
	private static final int MAX_HISTORY_SIZE = Math.max(IDLE_TRANSACTION_TIMEOUT_MIN, IDLE_BLOCK_TIMEOUT_MIN);

	public static TextView progressBar;

	private final AbstractWalletCoinListener WALLET_COIN_LISTENER = new AbstractWalletCoinListener() {
		@Override
		public void onCoinsReceived(Wallet wallet, final Transaction tx, Coin coin, Coin coin1) {
            org.creativecoinj.core.Context.propagate(CONTEXT);

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
					final boolean replaying = bestChainHeight < Configuration.getInstance().getBestChainHeightEver();
					final boolean isReplayedTx = confidenceType == ConfidenceType.BUILDING && replaying;

					if (isReceived && !isReplayedTx) {
						//NOTIFY INPUT TRANSACTION
						String id = tx.getHashAsString();
						notifyCoinsReceived(amount, id, addresses);
						broadcastTransactionReceived(id);
					}
				}
			});
		}

		@Override
		public void onCoinsSent(Wallet wallet, Transaction tx, Coin coin, Coin coin1) {
			transactionsReceived.incrementAndGet();
			String id = tx.getHashAsString();
			broadcastTransactionSend(id);
		}
	};

	private final Set<BlockchainState.Impediment> impediments = EnumSet.noneOf(BlockchainState.Impediment.class);
	private final Handler handler = new Handler();
	private final Handler delayHandler = new Handler();
	private final Handler rateHandler = new Handler();
	private final List<Address> notificationAddresses = new LinkedList<>();

	private WalletApplication application;

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

	private void broadcastTransactionSend(String id) {
		notifyTransaction(id, BlockchainBroadcastReceiver.TRANSACTION_SENT);
	}

	private void broadcastTransactionReceived(String id) {
		notifyTransaction(id, BlockchainBroadcastReceiver.TRANSACTION_RECEIVED);
	}

	private void notifyTransaction(String id, String action) {
		Intent sendIntent = new Intent(action);
		sendIntent.putExtra("txId", id);
		sendBroadcast(sendIntent);
	}

	private void notifyCoinsReceived(final Coin amount, String id, @Nullable final Address... addresses) {
		if (!Configuration.getInstance().isNotificationsEnabled()) {
			return;
		}

		if (notificationCount == 1) {
			nm.cancel(NOTIFICATION_ID_COINS_RECEIVED);
		}

		notificationCount++;
		notificationAccumulatedAmount = notificationAccumulatedAmount.add(amount);

		String person = TextUtils.join(",", WalletUtils.getAddressStrings(addresses));

		Coin coin = Coin.valueOf(notificationAccumulatedAmount.getValue());
		String msg;
		Intent intent;

		if (notificationCount < 2) {
			msg = getString(R.string.notif_cash_in, coin.toFriendlyString(), person);
			intent = new Intent(this, CoinTransactionActivity.class);
		} else {
			msg = getString(R.string.notif_accumulated_amount, coin.toFriendlyString(), notificationCount);
			intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		}

		intent.putExtra(TransactionActivity.TRANSACTION_ID, id);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		String title = getString(R.string.notif_cash_in_title, coin.toFriendlyString());
		final NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
		notification.setSmallIcon(R.mipmap.ic_notif);
		notification.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
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
			Configuration.getInstance().registerOnSharedPreferenceChangeListener(this);
		}

		public void stop() {
			stopped.set(true);
			Configuration.getInstance().unregisterOnSharedPreferenceChangeListener(this);

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

		private int remain;
		private String remainingTime = "";

        private int downloaded = 0;
        private int time = 0;
		private int blockSpeed = 0;
        private boolean rateStarted = false;

        @Override
        public void onChainDownloadStarted(Peer peer, int blocksLeft) {
            peerGroup.setMinBroadcastConnections(1);
            broadcastPendingTx();
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

                        Log.d(TAG, remainingTime + ", REMAINS=" + remain + ", " + blockSpeed + " blocks/s");
                        blockSpeed = 0;
                        if (progressBar != null) {
                            progressBar.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setText(getString(R.string.sync_time, remainingTime));
                                }
                            });
                        }
                        rateHandler.postDelayed(this, 1000);
                    }
                });
            }
            super.onChainDownloadStarted(peer, blocksLeft);
        }

        @Override
        protected void doneDownload() {
            Log.d(TAG, "doneDownload");
            Coin estimated = WalletHelper.INSTANCE.getTotalBalance(Wallet.BalanceType.ESTIMATED);
            Coin available = WalletHelper.INSTANCE.getTotalBalance(Wallet.BalanceType.AVAILABLE);
            Coin pending = estimated.minus(available);

            Log.d(TAG, "ESTIMATED = " + estimated.toFriendlyString());
            Log.d(TAG, "AVAILABLE = " + available.toFriendlyString());
            Log.d(TAG, "PENDING = " + pending.toFriendlyString());
            //WalletHelper.INSTANCE.cleanup();
            sendBroadcast(new Intent(BlockchainBroadcastReceiver.LAST_BLOCK_RECEIVED));
            rateHandler.removeCallbacksAndMessages(null);
            delayHandler.removeCallbacksAndMessages(null);
            Configuration.getInstance().maybeIncrementBestChainHeightEver(blockChain.getChainHead().getHeight());

        }

        @Override
		public void onBlocksDownloaded(final Peer peer, final Block block, final FilteredBlock filteredBlock, final int blocksLeft) {
			super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
			this.remain = blocksLeft;
			blockSpeed++;

			delayHandler.removeCallbacksAndMessages(null);
//			refreshConfirmations(height, block);

			final long now = System.currentTimeMillis();

			if (now - lastMessageTime.get() > CALLBACK_TIME) {
				delayHandler.post(RUNNER);
			} else {
				delayHandler.postDelayed(RUNNER, CALLBACK_TIME);
			}
		}

		private final Runnable RUNNER = new Runnable() {
			@Override
			public void run() {
                lastMessageTime.set(System.currentTimeMillis());
				Configuration.getInstance().maybeIncrementBestChainHeightEver(blockChain.getChainHead().getHeight());
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

                org.creativecoinj.core.Context.propagate(CONTEXT);

				Log.i(TAG, "starting peergroup");
				peerGroup = new PeerGroup(NETWORK_PARAMETERS, blockChain);
				peerGroup.setDownloadTxDependencies(0); // recursive implementation causes StackOverflowError
				peerGroup.setBloomFilteringEnabled(true);
				addWallets();

				peerGroup.setUserAgent(Constants.APP.CLIENT_NAME, Constants.APP.VERSION);
				peerGroup.addConnectedEventListener(peerConnectivityListener);
				peerGroup.addDisconnectedEventListener(peerConnectivityListener);

				final int maxConnectedPeers = application.maxConnectedPeers();

				peerGroup.setMaxConnections(maxConnectedPeers);
				peerGroup.setConnectTimeoutMillis(Constants.PEER_TIMEOUT_MS);
				peerGroup.setPeerDiscoveryTimeoutMillis(Constants.PEER_DISCOVERY_TIMEOUT_MS);

				peerGroup.addPeerDiscovery(new SeedPeers(NETWORK_PARAMETERS));

				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// start peergroup
						Log.e(TAG,"Starting blockchain download...");
						peerGroup.startAsync();
						peerGroup.startBlockChainDownload(blockchainDownloadListener);
						return null;
					}

					@Override
					protected void onPostExecute(Void aVoid) {
						super.onPostExecute(aVoid);
						broadcastSyncStarted();
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
				boolean isIdle = false;
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
				if (Configuration.getInstance().isIdleDetectionEnabled() && isIdle)	{
					Log.i(TAG, "idling detected, stopping service");
					stopSelf();
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
			return CreativeCoinService.this;
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

		boolean startService = WalletHelper.INSTANCE != null;

		Log.e(TAG,"START_SERVICE=" + startService);
		if (startService) {
			nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            WalletHelper.INSTANCE.autoSave(10);
			final String lockName = getPackageName() + " blockchain sync";

			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);
			application = WalletApplication.INSTANCE;

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
				if (!blockChainFileExists && earliestKeyCreationTime > 0){
					try	{
						Configuration.getInstance().setDeletingBlockchain(true);
						final long start = System.currentTimeMillis();
						final InputStream checkpointsInputStream = getAssets().open("bitcoin/" + Constants.WALLET.CHECKPOINTS_FILENAME);
						CheckpointManager.checkpoint(NETWORK_PARAMETERS, checkpointsInputStream, blockStore, earliestKeyCreationTime);
						Log.e(TAG,String.format("checkpoints loaded from '%1$s', took %2$dms", Constants.WALLET.CHECKPOINTS_FILENAME, System.currentTimeMillis() - start));
					} catch (final IOException x) {
						Log.e(TAG, "problem reading checkpoints, continuing without", x);
					}
				}
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
        Log.e(TAG, "onStartCommand");

        org.creativecoinj.core.Context.propagate(CONTEXT);
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
                broadcastTransactionPendingTx(intent);
            } else if (ACTION_RESET_BLOCKCHAIN.equals(action)) {
                Log.i(TAG, "will remove blockchain on service shutdown");

                resetBlockchainOnShutdown = true;
                stopSelf();
            } else if (peerGroup != null && peerGroup.isRunning()) {
                broadcastSyncStarted();
            }
        } else {
            Log.e(TAG,"service restart, although it was started as non-sticky");
        }
		return START_NOT_STICKY;
	}


	@Override
	public void onDestroy()	{
		Log.d(TAG, ".onDestroy()");

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
		handler.removeCallbacksAndMessages(null);
		rateHandler.removeCallbacksAndMessages(null);

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

		super.onDestroy();


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
		final boolean replaying = chainHead.getHeight() < Configuration.getInstance().getBestChainHeightEver();

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

	public void broadcastTransactionPendingTx(Intent data) {
		if (peerGroup != null) {
			final Sha256Hash hash = Sha256Hash.wrap(data.getByteArrayExtra(ACTION_BROADCAST_TRANSACTION_HASH));
			final Transaction tx = WalletHelper.INSTANCE.getTransaction(hash);
			Log.i(TAG, "broadcasting transaction " + tx.getHash().toString());
			peerGroup.broadcastTransaction(tx);

		} else {
			Log.i(TAG, "Tx not available");
		}
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

	private void broadcastPendingTx() {
        List<Transaction> pendings = WalletHelper.INSTANCE.getPendingTransactions();
        Log.d(TAG, "broadcasting " + pendings.size() + " transactions");
        for (Transaction tx : pendings) {
            peerGroup.broadcastTransaction(tx);
        }
    }

    private void broadcastSyncStarted() {
		sendBroadcast(new Intent(ACTION_SYNC_STARTED));
	}
}
