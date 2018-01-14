/*
 * Copyright 2012-2015 the original author or authors.
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

import org.creativecoinj.core.Peer;
import org.creativecoinj.core.StoredBlock;

import java.util.List;

import javax.annotation.Nullable;

/**
 * @author Andreas Schildbach
 */
public interface BlockchainService {
	String ACTION_PEER_STATE = BlockchainService.class.getPackage().getName() + ".peer_state";
	String ACTION_PEER_STATE_NUM_PEERS = "num_peers";

	String ACTION_BLOCKCHAIN_STATE = BlockchainService.class.getPackage().getName() + ".blockchain_state";

	String ACTION_CANCEL_COINS_RECEIVED = BlockchainService.class.getPackage().getName() + ".cancel_coins_received";
	String ACTION_RESET_BLOCKCHAIN = BlockchainService.class.getPackage().getName() + ".reset_blockchain";
	String ACTION_SEND_PEERS = BlockchainService.class.getPackage().getName() + ".send_peers";
	String ACTION_BROADCAST_TRANSACTION = BlockchainService.class.getPackage().getName() + ".broadcast_transaction";
	String ACTION_BROADCAST_RAW_TRANSACTION = "rawTx";

	BlockchainState getBlockchainState();

	@Nullable
	List<Peer> getConnectedPeers();

	List<StoredBlock> getRecentBlocks(int maxBlocks);
}
