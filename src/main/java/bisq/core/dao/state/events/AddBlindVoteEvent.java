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

package bisq.core.dao.state.events;


import bisq.core.dao.vote.blindvote.BlindVote;

import com.google.protobuf.Message;

import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.annotation.concurrent.Immutable;

@Immutable
@EqualsAndHashCode(callSuper = true)
@Value
public class AddBlindVoteEvent extends StateChangeEvent {

    public AddBlindVoteEvent(BlindVote payload, int height) {
        super(payload, height);
    }

    //TODO
    @Override
    public Message toProtoMessage() {
        return null;
    }


    public BlindVote getBlindVote() {
        return (BlindVote) getPayload();
    }
}
