/*
 * This file is part of bisq.
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

package bisq.core.dao.vote;

import bisq.core.app.BisqEnvironment;
import bisq.core.btc.exceptions.TransactionVerificationException;
import bisq.core.btc.exceptions.WalletException;
import bisq.core.btc.wallet.BsqWalletService;
import bisq.core.btc.wallet.BtcWalletService;
import bisq.core.btc.wallet.ChangeBelowDustException;
import bisq.core.dao.DaoPeriodService;
import bisq.core.dao.blockchain.ReadableBsqBlockChain;
import bisq.core.dao.proposal.Proposal;
import bisq.core.dao.proposal.ProposalCollectionsService;
import bisq.core.dao.proposal.ProposalList;
import bisq.core.dao.vote.consensus.VoteConsensus;

import bisq.network.p2p.P2PService;
import bisq.network.p2p.storage.HashMapChangedListener;
import bisq.network.p2p.storage.payload.ProtectedStorageEntry;
import bisq.network.p2p.storage.payload.ProtectedStoragePayload;

import bisq.common.UserThread;
import bisq.common.app.DevEnv;
import bisq.common.crypto.CryptoException;
import bisq.common.crypto.Encryption;
import bisq.common.crypto.KeyRing;
import bisq.common.proto.persistable.PersistedDataHost;
import bisq.common.storage.Storage;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;

import javax.inject.Inject;

import com.google.common.util.concurrent.FutureCallback;

import javafx.beans.value.ChangeListener;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javax.crypto.SecretKey;

import java.security.PublicKey;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates and published blind votes and manages the vote list.
 */
@Slf4j
public class VoteService implements PersistedDataHost, HashMapChangedListener {
    private final ProposalCollectionsService proposalCollectionsService;
    private final ReadableBsqBlockChain readableBsqBlockChain;
    private DaoPeriodService daoPeriodService;
    private final BsqWalletService bsqWalletService;
    private final BtcWalletService btcWalletService;
    private final P2PService p2PService;
    private final PublicKey signaturePubKey;
    private final Storage<VoteList> voteListStorage;

    @Getter
    private final List<Vote> voteList = new ArrayList<>();
    @Getter
    private final ObservableList<BlindVote> blindVoteList = FXCollections.observableArrayList();
    private final List<BlindVote> blindVoteSortedList = new SortedList<>(blindVoteList);
    private ChangeListener<Number> numConnectedPeersListener;

    @Inject
    public VoteService(ProposalCollectionsService proposalCollectionsService,
                       ReadableBsqBlockChain readableBsqBlockChain,
                       DaoPeriodService daoPeriodService,
                       BsqWalletService bsqWalletService,
                       BtcWalletService btcWalletService,
                       P2PService p2PService,
                       KeyRing keyRing,
                       Storage<VoteList> voteListStorage) {
        this.proposalCollectionsService = proposalCollectionsService;
        this.readableBsqBlockChain = readableBsqBlockChain;
        this.daoPeriodService = daoPeriodService;
        this.bsqWalletService = bsqWalletService;
        this.btcWalletService = btcWalletService;
        this.p2PService = p2PService;

        signaturePubKey = keyRing.getPubKeyRing().getSignaturePubKey();
        this.voteListStorage = voteListStorage;

        blindVoteSortedList.sort(VoteConsensus.getBlindVoteListComparator());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PersistedDataHost
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void readPersisted() {
        if (BisqEnvironment.isDAOActivatedAndBaseCurrencySupportingBsq()) {
            VoteList persisted = voteListStorage.initAndGetPersistedWithFileName("VoteList", 100);
            if (persisted != null) {
                this.voteList.clear();
                this.voteList.addAll(persisted.getList());
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // HashMapChangedListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAdded(ProtectedStorageEntry data) {
        final ProtectedStoragePayload protectedStoragePayload = data.getProtectedStoragePayload();
        if (protectedStoragePayload instanceof BlindVote) {
            addBlindVote((BlindVote) protectedStoragePayload);
        }
    }

    @Override
    public void onRemoved(ProtectedStorageEntry data) {
        // Removal is not implemented
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void onAllServicesInitialized() {
        p2PService.addHashSetChangedListener(this);

        // At startup the P2PDataStorage initializes earlier, otherwise we get the listener called.
        p2PService.getP2PDataStorage().getMap().values().forEach(e -> {
            final ProtectedStoragePayload protectedStoragePayload = e.getProtectedStoragePayload();
            if (protectedStoragePayload instanceof BlindVote)
                addBlindVote((BlindVote) protectedStoragePayload);
        });

        // Republish own active blindVotes once we are well connected
        numConnectedPeersListener = (observable, oldValue, newValue) -> {
            UserThread.runAfter(() -> rebroadcastBlindVotes((int) newValue), 2);
        };
        p2PService.getNumConnectedPeers().addListener(numConnectedPeersListener);
    }

    private void rebroadcastBlindVotes(int numConnectedPeers) {
        if ((numConnectedPeers > 4 && p2PService.isBootstrapped()) || DevEnv.isDevMode()) {
            p2PService.getNumConnectedPeers().removeListener(numConnectedPeersListener);
            voteList.stream()
                    .filter(vote -> daoPeriodService.isTxInPhase(vote.getBlindVote().getTxId(),
                            DaoPeriodService.Phase.OPEN_FOR_VOTING))
                    .forEach(vote -> addBlindVoteToP2PNetwork(vote.getBlindVote()));
        }
    }

    public void shutDown() {
    }

    public void publishBlindVote(Coin stake, FutureCallback<Transaction> callback) throws CryptoException,
            InsufficientMoneyException,
            ChangeBelowDustException, WalletException, TransactionVerificationException {
        ProposalList proposalList = getProposalList(proposalCollectionsService.getActiveProposals());
        SecretKey secretKey = VoteConsensus.getSecretKey();
        byte[] encryptedProposalList = getEncryptedVoteList(proposalList, secretKey);
        byte[] opReturnData = VoteConsensus.getOpReturnData(encryptedProposalList);
        final Transaction blindVoteTx = getBlindVoteTx(stake, opReturnData);
        BlindVote blindVote = new BlindVote(encryptedProposalList, blindVoteTx.getHashAsString(), signaturePubKey);

        publishTx(blindVoteTx, new FutureCallback<Transaction>() {
            @Override
            public void onSuccess(@Nullable Transaction result) {
                addBlindVoteToP2PNetwork(blindVote);
                Vote vote = new Vote(proposalList, Utils.HEX.encode(secretKey.getEncoded()), blindVote);
                voteList.add(vote);
                voteListStorage.queueUpForSave(new VoteList(voteList), 100);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void publishTx(Transaction blindVoteTx, FutureCallback<Transaction> callback) {
        // We need to create another instance, otherwise the tx would trigger an invalid state exception
        // if it gets committed 2 times
        // We clone before commit to avoid unwanted side effects
        final Transaction clonedTx = btcWalletService.getClonedTransaction(blindVoteTx);
        btcWalletService.commitTx(clonedTx);

        bsqWalletService.commitTx(blindVoteTx);

        bsqWalletService.broadcastTx(blindVoteTx, new FutureCallback<Transaction>() {
            @Override
            public void onSuccess(@Nullable Transaction transaction) {
                checkNotNull(transaction, "Transaction must not be null at broadcastTx callback.");

                callback.onSuccess(transaction);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void addBlindVote(BlindVote blindVote) {
        if (!blindVoteList.contains(blindVote))
            blindVoteList.add(blindVote);
        else
            log.warn("We have that item in our list already");
    }

    private ProposalList getProposalList(FilteredList<Proposal> proposals) {
        final List<Proposal> clonedProposals = new ArrayList<>(proposals);
        final List<Proposal> sortedProposals = VoteConsensus.getSortedProposalList(clonedProposals);
        return new ProposalList(sortedProposals);
    }

    // TODO add tests
    private byte[] getEncryptedVoteList(ProposalList proposalList, SecretKey secretKey) throws CryptoException {
        byte[] proposalListAsBytes = VoteConsensus.getProposalListAsByteArray(proposalList);

      /*  byte[] decryptedProposalList = Encryption.decrypt(encryptedProposalList, secretKey);
        try {
            PB.PersistableEnvelope proto = PB.PersistableEnvelope.parseFrom(decryptedProposalList);
            PersistableEnvelope decrypted = ProposalList.fromProto(proto.getProposalList());
            log.error(decrypted.toString());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }*/

        return Encryption.encrypt(proposalListAsBytes, secretKey);
    }

    private Transaction getBlindVoteTx(Coin stake, byte[] opReturnData)
            throws InsufficientMoneyException, ChangeBelowDustException, WalletException,
            TransactionVerificationException {
        final Coin voteFee = VoteConsensus.getVoteFee(readableBsqBlockChain);
        Transaction preparedVoteFeeTx = bsqWalletService.getPreparedVoteFeeTx(voteFee, stake);
        checkArgument(!preparedVoteFeeTx.getInputs().isEmpty(), "preparedVoteFeeTx inputs must not be empty");
        checkArgument(!preparedVoteFeeTx.getOutputs().isEmpty(), "preparedVoteFeeTx outputs must not be empty");
        Transaction txWithBtcFee = btcWalletService.completePreparedVoteTx(preparedVoteFeeTx, opReturnData);
        return bsqWalletService.signTx(txWithBtcFee);
    }

    private void addBlindVoteToP2PNetwork(BlindVote blindVote) {
        p2PService.addProtectedStorageEntry(blindVote, true);
    }
}
