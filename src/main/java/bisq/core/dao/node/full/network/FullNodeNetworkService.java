/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.dao.node.full.network;

import bisq.core.dao.node.messages.GetBlocksRequest;
import bisq.core.dao.node.messages.NewBlockBroadcastMessage;
import bisq.core.dao.state.StateService;
import bisq.core.dao.state.blockchain.Block;
import bisq.core.dao.state.blockchain.RawBlock;

import bisq.network.p2p.network.Connection;
import bisq.network.p2p.network.MessageListener;
import bisq.network.p2p.network.NetworkNode;
import bisq.network.p2p.peers.Broadcaster;
import bisq.network.p2p.peers.PeerManager;

import bisq.common.UserThread;
import bisq.common.app.Log;
import bisq.common.proto.network.NetworkEnvelope;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.Nullable;

/**
 * Responsible for handling requests for BSQ blocks from lite nodes and for broadcasting new blocks to the P2P network.
 */
@Slf4j
public class FullNodeNetworkService implements MessageListener, PeerManager.Listener {

    private static final long CLEANUP_TIMER = 120;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Class fields
    ///////////////////////////////////////////////////////////////////////////////////////////

    private final NetworkNode networkNode;
    private final PeerManager peerManager;
    private final Broadcaster broadcaster;
    private final StateService stateService;

    // Key is connection UID
    private final Map<String, GetBlocksRequestHandler> getBlocksRequestHandlers = new HashMap<>();
    private boolean stopped;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public FullNodeNetworkService(NetworkNode networkNode,
                                  PeerManager peerManager,
                                  Broadcaster broadcaster,
                                  StateService stateService) {
        this.networkNode = networkNode;
        this.peerManager = peerManager;
        this.broadcaster = broadcaster;
        this.stateService = stateService;

        networkNode.addMessageListener(this);
        peerManager.addListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("Duplicates")
    public void shutDown() {
        Log.traceCall();
        stopped = true;
        networkNode.removeMessageListener(this);
        peerManager.removeListener(this);
    }

    public void publishNewBlock(Block block) {
        log.info("Publish new block at height={} and block hash={}", block.getHeight(), block.getHash());
        RawBlock rawBlock = RawBlock.fromBlock(block);
        final NewBlockBroadcastMessage newBlockBroadcastMessage = new NewBlockBroadcastMessage(rawBlock);
        broadcaster.broadcast(newBlockBroadcastMessage, networkNode.getNodeAddress(), null, true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PeerManager.Listener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAllConnectionsLost() {
        stopped = true;
    }

    @Override
    public void onNewConnectionAfterAllConnectionsLost() {
        stopped = false;
    }

    @Override
    public void onAwakeFromStandby() {
        stopped = false;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkEnvelope networkEnvelope, Connection connection) {
        if (networkEnvelope instanceof GetBlocksRequest) {
            // We received a GetBlocksRequest from a liteNode
            Log.traceCall(networkEnvelope.toString() + "\n\tconnection=" + connection);
            if (!stopped) {
                final String uid = connection.getUid();
                if (!getBlocksRequestHandlers.containsKey(uid)) {
                    GetBlocksRequestHandler requestHandler = new GetBlocksRequestHandler(networkNode,
                            stateService,
                            new GetBlocksRequestHandler.Listener() {
                                @Override
                                public void onComplete() {
                                    getBlocksRequestHandlers.remove(uid);
                                    log.trace("requestDataHandshake completed.\n\tConnection={}", connection);
                                }

                                @Override
                                public void onFault(String errorMessage, @Nullable Connection connection) {
                                    getBlocksRequestHandlers.remove(uid);
                                    if (!stopped) {
                                        log.trace("GetDataRequestHandler failed.\n\tConnection={}\n\t" +
                                                "ErrorMessage={}", connection, errorMessage);
                                        peerManager.handleConnectionFault(connection);
                                    } else {
                                        log.warn("We have stopped already. We ignore that getDataRequestHandler.handle.onFault call.");
                                    }
                                }
                            });
                    getBlocksRequestHandlers.put(uid, requestHandler);
                    requestHandler.onGetBlocksRequest((GetBlocksRequest) networkEnvelope, connection);
                } else {
                    log.warn("We have already a GetDataRequestHandler for that connection started. " +
                            "We start a cleanup timer if the handler has not closed by itself in between 2 minutes.");

                    UserThread.runAfter(() -> {
                        if (getBlocksRequestHandlers.containsKey(uid)) {
                            GetBlocksRequestHandler handler = getBlocksRequestHandlers.get(uid);
                            handler.stop();
                            getBlocksRequestHandlers.remove(uid);
                        }
                    }, CLEANUP_TIMER);
                }
            } else {
                log.warn("We have stopped already. We ignore that onMessage call.");
            }
        }
    }
}
