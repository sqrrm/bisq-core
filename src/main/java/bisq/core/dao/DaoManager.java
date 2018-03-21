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

package bisq.core.dao;

import bisq.core.app.BisqEnvironment;
import bisq.core.dao.node.BsqNode;
import bisq.core.dao.node.BsqNodeProvider;
import bisq.core.dao.proposal.ProposalCollectionsService;
import bisq.core.dao.vote.VoteService;

import bisq.common.handlers.ErrorMessageHandler;

import com.google.inject.Inject;

/**
 * High level entry point for Dao domain
 */
public class DaoManager {
    private final DaoPeriodService daoPeriodService;
    private final ProposalCollectionsService proposalCollectionsService;
    private final BsqNode bsqNode;
    private final VoteService voteService;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public DaoManager(BsqNodeProvider bsqNodeProvider,
                      DaoPeriodService daoPeriodService,
                      ProposalCollectionsService proposalCollectionsService,
                      VoteService voteService) {
        this.daoPeriodService = daoPeriodService;
        this.proposalCollectionsService = proposalCollectionsService;
        this.voteService = voteService;

        bsqNode = bsqNodeProvider.getBsqNode();
    }

    public void onAllServicesInitialized(ErrorMessageHandler errorMessageHandler) {
        if (BisqEnvironment.isDAOActivatedAndBaseCurrencySupportingBsq()) {
            daoPeriodService.onAllServicesInitialized();
            proposalCollectionsService.onAllServicesInitialized();
            bsqNode.onAllServicesInitialized(errorMessageHandler);
            voteService.onAllServicesInitialized();
        }
    }

    public void shutDown() {
        daoPeriodService.shutDown();
        proposalCollectionsService.shutDown();
        bsqNode.shutDown();
        voteService.shutDown();
    }
}
