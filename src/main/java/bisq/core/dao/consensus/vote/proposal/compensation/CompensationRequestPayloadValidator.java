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

package bisq.core.dao.consensus.vote.proposal.compensation;

import bisq.core.dao.consensus.vote.proposal.Proposal;
import bisq.core.dao.consensus.vote.proposal.ProposalPayloadValidator;
import bisq.core.dao.consensus.vote.proposal.ValidationException;

import org.bitcoinj.core.Coin;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import static bisq.core.dao.consensus.vote.proposal.compensation.CompensationRequestConsensus.getMaxCompensationRequestAmount;
import static bisq.core.dao.consensus.vote.proposal.compensation.CompensationRequestConsensus.getMinCompensationRequestAmount;
import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.Validate.notEmpty;

@Slf4j
public class CompensationRequestPayloadValidator extends ProposalPayloadValidator {

    @Inject
    public CompensationRequestPayloadValidator() {
        super();
    }

    @Override
    public void validateDataFields(Proposal proposal) throws ValidationException {
        try {
            super.validateDataFields(proposal);

            CompensationRequestProposal compensationRequestProposal = (CompensationRequestProposal) proposal;
            String bsqAddress = compensationRequestProposal.getBsqAddress();
            notEmpty(bsqAddress, "bsqAddress must not be empty");
            checkArgument(bsqAddress.substring(0, 1).equals("B"), "bsqAddress must start with B");
            compensationRequestProposal.getAddress(); // throws AddressFormatException if wrong address
            final Coin requestedBsq = compensationRequestProposal.getRequestedBsq();
            checkArgument(requestedBsq.compareTo(getMaxCompensationRequestAmount()) <= 0,
                    "Requested BSQ must not exceed MaxCompensationRequestAmount");
            checkArgument(requestedBsq.compareTo(getMinCompensationRequestAmount()) >= 0,
                    "Requested BSQ must not be less than MinCompensationRequestAmount");
        } catch (Throwable throwable) {
            throw new ValidationException(throwable);
        }
    }
}