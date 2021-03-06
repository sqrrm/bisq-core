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

package io.bisq.core.dao.blockchain.vo;

import io.bisq.common.proto.ProtoUtil;
import io.bisq.generated.protobuffer.PB;

public enum TxType {
    UNDEFINED_TX_TYPE,
    UNVERIFIED,
    INVALID,
    GENESIS,
    TRANSFER_BSQ,
    PAY_TRADE_FEE,
    COMPENSATION_REQUEST,
    VOTE,
    VOTE_REVEAL,
    ISSUANCE,
    LOCK_UP,
    UN_LOCK;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    public static TxType fromProto(PB.TxType txType) {
        return ProtoUtil.enumFromProto(TxType.class, txType.name());
    }

    public PB.TxType toProtoMessage() {
        return PB.TxType.valueOf(name());
    }
}
