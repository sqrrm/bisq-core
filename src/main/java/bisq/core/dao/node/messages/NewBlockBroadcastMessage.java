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

package bisq.core.dao.node.messages;

import bisq.core.dao.state.blockchain.Block;

import bisq.network.p2p.storage.messages.BroadcastMessage;

import bisq.common.app.Version;
import bisq.common.proto.network.NetworkEnvelope;

import io.bisq.generated.protobuffer.PB;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public final class NewBlockBroadcastMessage extends BroadcastMessage {
    private final Block block;

    public NewBlockBroadcastMessage(Block block) {
        this(block, Version.getP2PMessageVersion());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    private NewBlockBroadcastMessage(Block block, int messageVersion) {
        super(messageVersion);
        this.block = block;
    }

    @Override
    public PB.NetworkEnvelope toProtoNetworkEnvelope() {
        return getNetworkEnvelopeBuilder()
                .setNewBlockBroadcastMessage(PB.NewBlockBroadcastMessage.newBuilder()
                        .setBlock(block.toProtoMessage()))
                .build();
    }

    public static NetworkEnvelope fromProto(PB.NewBlockBroadcastMessage proto, int messageVersion) {
        return new NewBlockBroadcastMessage(Block.fromProto(proto.getBlock()),
                messageVersion);
    }
}