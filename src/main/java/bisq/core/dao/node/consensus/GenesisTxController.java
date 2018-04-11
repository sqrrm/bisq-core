/*
 * This file is part of Bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.dao.node.consensus;

import bisq.core.dao.DaoOptionKeys;
import bisq.core.dao.blockchain.vo.Tx;
import bisq.core.dao.blockchain.vo.TxType;
import bisq.core.dao.state.ChainStateService;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Verifies if a given transaction is a BSQ genesis transaction.
 */
public class GenesisTxController {

    private final ChainStateService chainStateService;
    private final GenesisTxOutputController genesisTxOutputController;
    private final String genesisTxId;
    private final int genesisBlockHeight;

    @Inject
    public GenesisTxController(ChainStateService chainStateService,
                               GenesisTxOutputController genesisTxOutputController,
                               @Named(DaoOptionKeys.GENESIS_TX_ID) String genesisTxId,
                               @Named(DaoOptionKeys.GENESIS_BLOCK_HEIGHT) int genesisBlockHeight) {
        this.chainStateService = chainStateService;
        this.genesisTxOutputController = genesisTxOutputController;
        this.genesisTxId = genesisTxId;
        this.genesisBlockHeight = genesisBlockHeight;
    }

    public boolean isGenesisTx(Tx tx, int blockHeight) {
        return tx.getId().equals(genesisTxId) && blockHeight == genesisBlockHeight;
    }

    public void applyStateChange(Tx tx) {
        Model model = new Model(chainStateService.getIssuedAmountAtGenesis().getValue());
        for (int i = 0; i < tx.getOutputs().size(); ++i) {
            genesisTxOutputController.verify(tx.getOutputs().get(i), model);
        }

        tx.setTxType(TxType.GENESIS);
        chainStateService.setGenesisTx(tx);
        chainStateService.addTxToMap(tx);
    }
}
