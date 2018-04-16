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

package bisq.core.dao.vote.period;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.Value;

import javax.annotation.concurrent.Immutable;

/**
 * Cycle represents the monthly period for proposals and voting.
 * It consists of a ordered list of phases represented in the phaseWrappers.
 *
 */
//TODO add tests
@Immutable
@Value
public class Cycle {
    // List is ordered according to the Phase enum.
    private final List<PhaseWrapper> phaseWrappers;
    private final int heightOfFirstBlock;

    Cycle(int heightOfFirstBlock) {
        this(heightOfFirstBlock, new ArrayList<>());
    }

    Cycle(int heightOfFirstBlock, List<PhaseWrapper> phaseWrappers) {
        this.heightOfFirstBlock = heightOfFirstBlock;
        this.phaseWrappers = phaseWrappers;
    }

    void setPhaseWrapper(PhaseWrapper phaseWrapper) {
        getPhaseWrapper(phaseWrapper.getPhase()).ifPresent(phaseWrappers::remove);
        phaseWrappers.add(phaseWrapper);
    }


    int getHeightOfLastBlock() {
        return heightOfFirstBlock + getDuration() - 1;
    }

    boolean isInPhase(int height, Phase phase) {
        return height >= getFirstBlockOfPhase(phase) &&
                height <= getLastBlockOfPhase(phase);
    }

    int getFirstBlockOfPhase(Phase phase) {
        return heightOfFirstBlock + phaseWrappers.stream()
                .filter(item -> item.getPhase().ordinal() < phase.ordinal())
                .mapToInt(PhaseWrapper::getDuration).sum();
    }

    int getLastBlockOfPhase(Phase phase) {
        return getFirstBlockOfPhase(phase) + getDuration(phase) - 1;
    }

    int getDurationOfPhase(Phase phase) {
        return phaseWrappers.stream()
                .filter(item -> item.getPhase() == phase)
                .mapToInt(PhaseWrapper::getDuration).sum();
    }

    Optional<Phase> getPhaseForHeight(int height) {
        return phaseWrappers.stream()
                .filter(item -> isInPhase(height, item.getPhase()))
                .map(PhaseWrapper::getPhase)
                .findAny();
    }


    private Optional<PhaseWrapper> getPhaseWrapper(Phase phase) {
        return phaseWrappers.stream().filter(item -> item.getPhase() == phase).findAny();
    }

    private int getDuration(Phase phase) {
        return getPhaseWrapper(phase).map(PhaseWrapper::getDuration).orElse(0);
    }

    private int getDuration() {
        return phaseWrappers.stream().mapToInt(PhaseWrapper::getDuration).sum();
    }

    @Override
    public String toString() {
        return "Cycle{" +
                "\n     phaseWrappers=" + phaseWrappers +
                ",\n     heightOfFirstBlock=" + heightOfFirstBlock +
                "\n}";
    }
}
