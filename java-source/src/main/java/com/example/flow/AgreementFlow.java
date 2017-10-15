package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.AgreementContract;
import com.example.state.AgreementState;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionType;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.FinalityFlow;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Set;

public class AgreementFlow {
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final AgreementState agreementState;
        private final Party otherParty;

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                SENDING_TRANSACTION
        );

        private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step(
                "Generating transaction based on new IOU.");
        private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
                "Verifying contract constraints.");
        private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step(
                "Signing transaction with our private key.");
        private static final ProgressTracker.Step SENDING_TRANSACTION = new ProgressTracker.Step(
                "Sending proposed transaction to seller for review.");

        public Initiator(AgreementState _agreementState, Party otherParty) {
            this.agreementState = _agreementState;
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            System.out.println("ExampleFlow call ");
            // Prep.
            // Obtain a reference to our key pair. Currently, the only key pair used is the one which is registered with
            // the NetWorkMapService. In a future milestone release we'll implement HD key generation such that new keys
            // can be generated for each transaction.
            final KeyPair keyPair = getServiceHub().getLegalIdentityKey();
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0).getNotaryIdentity();
            System.out.println("ExampleFlow notary.getName() "+notary.getName());
            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            final Command txCommand = new Command(new AgreementContract.Commands.Create(), agreementState.getParticipants());
            final TransactionBuilder unsignedTx = new TransactionType.General.Builder(notary).withItems(agreementState, txCommand);
            System.out.println("ExampleFlow txCommand "+txCommand.getValue());
            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            unsignedTx.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();
            System.out.println("ExampleFlow txCommand "+unsignedTx.inputStates());
            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction partSignedTx = unsignedTx.signWith(keyPair).toSignedTransaction(false);

            // Stage 4.
            progressTracker.setCurrentStep(SENDING_TRANSACTION);
            // Send the state across the wire to the designated counterparty.
            // -----------------------
            // Flow jumps to Acceptor.
            // -----------------------
            System.out.println("FlowLogic Just before sending "+partSignedTx.toString());
            this.send(otherParty, partSignedTx);

            return waitForLedgerCommit(partSignedTx.getId());
        }
    }

    public static class Acceptor extends FlowLogic<Void> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(
                RECEIVING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        private static final ProgressTracker.Step RECEIVING_TRANSACTION = new ProgressTracker.Step(
                "Receiving proposed transaction from buyer.");
        private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step(
                "Signing proposed transaction with our private key.");
        private static final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step(
                "Obtaining notary signature and recording transaction.");

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Prep.
            // Obtain a reference to our key pair.
            System.out.println("Acceptor Call "+getServiceHub().getClass().getName());
            System.out.println("getServiceHub().getNetworkMapCache() "+getServiceHub().getNetworkMapCache());
            System.out.println("getServiceHub().getNetworkMapCache().getNotaryNodes() "+getServiceHub().getNetworkMapCache().getNotaryNodes());
            final KeyPair keyPair = getServiceHub().getLegalIdentityKey();
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0).getNotaryIdentity();
            // Obtain a reference to the notary we want to use and its public key.
            final PublicKey notaryPubKey = notary.getOwningKey();

            // Stage 5.
            progressTracker.setCurrentStep(RECEIVING_TRANSACTION);
            // All messages come off the wire as UntrustworthyData. You need to 'unwrap' them. This is where you
            // validate what you have just received.

            final SignedTransaction partSignedTx = receive(SignedTransaction.class, otherParty)
                    .unwrap(tx ->
                    {
                        // Stage 6.
                        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
                        try {
                            // Check that the signature of the other party is valid.
                            // Our signature and the notary's signature are allowed to be omitted at this stage as
                            // this is only a partially signed transaction.
                            final WireTransaction wireTx = tx.verifySignatures(keyPair.getPublic(), notaryPubKey);

                            // Run the contract's verify function.
                            // We want to be sure that the agreed-upon IOU is valid under the rules of the contract.
                            // To do this we need to run the contract's verify() function.
                            wireTx.toLedgerTransaction(getServiceHub()).verify();
                        } catch (SignatureException ex) {
                            throw new FlowException(tx.getId() + " failed signature checks", ex);
                        }
                        return tx;
                    });

            // Stage 7.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction with our key pair and add it to the transaction.
            // We now have 'validation consensus'. We still require uniqueness consensus.
            // Technically validation consensus for this type of agreement implicitly provides uniqueness consensus.
            final DigitalSignature.WithKey mySig = partSignedTx.signWithECDSA(keyPair);
            // Add our signature to the transaction.
            final SignedTransaction signedTx = partSignedTx.plus(mySig);

            // Stage 8.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            final Set<Party> participants = ImmutableSet.of(getServiceHub().getMyInfo().getLegalIdentity(), otherParty);
            // FinalityFlow() notarises the transaction and records it in each party's vault.
            subFlow(new FinalityFlow(signedTx, participants));

            return null;
        }
    }
}
