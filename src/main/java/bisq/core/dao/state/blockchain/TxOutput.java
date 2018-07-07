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

package bisq.core.dao.state.blockchain;

import io.bisq.generated.protobuffer.PB;

import lombok.Data;
import lombok.experimental.Delegate;

/**
 * TxOutput containing BSQ specific data. The raw blockchain specific TxOutput data are in the rawTxOutput object.
 * We use lombok for convenience to delegate access to the data inside the rawTxOutput.
 */
@Data
public class TxOutput {
    private interface ExcludesDelegateMethods<T> {
        PB.TxOutput toProtoMessage();
    }

    @Delegate(excludes = TxOutput.ExcludesDelegateMethods.class)
    private final RawTxOutput rawTxOutput;
    private TxOutputType txOutputType = TxOutputType.UNDEFINED;
    private boolean isUnspent = false;

    public TxOutput(RawTxOutput rawTxOutput) {
        this.rawTxOutput = rawTxOutput;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    private TxOutput(RawTxOutput rawTxOutput, TxOutputType txOutputType, boolean isUnspent) {
        this.rawTxOutput = rawTxOutput;
        this.txOutputType = txOutputType;
        this.isUnspent = isUnspent;
    }

    public PB.TxOutput toProtoMessage() {
        final PB.TxOutput.Builder builder = PB.TxOutput.newBuilder()
                .setRawTxOutput(rawTxOutput.toProtoMessage())
                .setTxOutputType(txOutputType.toProtoMessage())
                .setIsUnspent(isUnspent);
        return builder.build();
    }

    public static TxOutput fromProto(PB.TxOutput proto) {
        return new TxOutput(RawTxOutput.fromProto(proto.getRawTxOutput()),
                TxOutputType.fromProto(proto.getTxOutputType()),
                proto.getIsUnspent());
    }

    @Override
    public String toString() {
        return "TxOutput{" +
                "\n     rawTxOutput=" + rawTxOutput +
                ",\n     txOutputType=" + txOutputType +
                ",\n     isUnspent=" + isUnspent +
                "\n}";
    }
}
