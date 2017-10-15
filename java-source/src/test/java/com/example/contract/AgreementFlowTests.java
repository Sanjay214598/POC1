package com.example.contract;

import com.example.flow.AgreementFlow;
import com.example.model.Agreement;
import com.example.state.AgreementState;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.FlowException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class AgreementFlowTests {
    private MockNetwork net;
    private MockNetwork.MockNode a;
    private MockNetwork.MockNode b;
    private MockNetwork.MockNode c;

    @Before
    public void setup() {
        net = new MockNetwork();
        MockNetwork.BasketOfNodes nodes = net.createSomeNodes(3);
        a = nodes.getPartyNodes().get(0);
        b = nodes.getPartyNodes().get(1);
        c = nodes.getPartyNodes().get(2);
        net.runNetwork();
    }

    @After
    public void tearDown() {
        net.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /*@Test
    public void flowRejectsInvalidAgreements() throws Exception {
        // The AgreementContract specifies that Agreements cannot have negative values.
        AgreementState state = new AgreementState(
                new Agreement("Aaa",
                        "Addd",
                        "addd",
                        "Addd",
                        "Addd",
                        "adddd",
                        "A",
                        "A",
                        "a"),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new AgreementContract());

        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }*/

    @Test
    public void flowRejectsInvalidAgreementStates() throws Exception {
        // The AgreementContract specifies that an Agreement's sender and recipient cannot be the same.
        AgreementState state = new AgreementState(
                new Agreement("A", "A","a", "A", "A","a", "A", "A","a"),
                a.info.getLegalIdentity(),
                a.info.getLegalIdentity(),
                new AgreementContract());

        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        AgreementState state = new AgreementState(
                new Agreement("A", "A","a", "A", "A","a", "A", "A","a"),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new AgreementContract());
        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignatures(b.getServices().getLegalIdentityKey().getPublic());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        AgreementState state = new AgreementState(
                new Agreement("A", "A","a", "A", "A","a", "A", "A","a"),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new AgreementContract());
        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignatures(a.getServices().getLegalIdentityKey().getPublic());
    }

    @Test
    public void flowRejectsAgreementsThatAreNotSignedByTheSender() throws Exception {
        AgreementState state = new AgreementState(
                new Agreement("A", "A","a", "A", "A","a", "A", "A","a"),
                c.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new AgreementContract());

        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(FlowException.class));
        future.get();
    }

    @Test
    public void flowRejectsAgreementsThatAreNotSignedByTheRecipient() throws Exception {
        AgreementState state = new AgreementState(
                new Agreement("A", "A","a", "A", "A","a", "A", "A","a"),
                a.info.getLegalIdentity(),
                c.info.getLegalIdentity(),
                new AgreementContract());

        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(FlowException.class));
        future.get();
    }

    @Test
    public void flowRecordsATransactionInBothPartiesVaults() throws Exception {
        AgreementState state = new AgreementState(
                new Agreement("A", "A","a", "A", "A","a", "A", "A","a"),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new AgreementContract());
        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();
        SignedTransaction signedTx = future.get();

        for (MockNetwork.MockNode node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.storage.getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputAgreement() throws Exception {
        AgreementState inputState = new AgreementState(
                new Agreement("A", "A","a", "A", "A","a", "A", "A","a"),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new AgreementContract());
        AgreementFlow.Initiator flow = new AgreementFlow.Initiator(inputState, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();
        SignedTransaction signedTx = future.get();

        for (MockNetwork.MockNode node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.storage.getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert(txOutputs.size() == 1);

            AgreementState recordedState = (AgreementState) txOutputs.get(0).getData();
            assertEquals(recordedState.getAgreement().getName(), inputState.getAgreement().getName());
            assertEquals(recordedState.getSender(), inputState.getSender());
            assertEquals(recordedState.getRecipient(), inputState.getRecipient());
            assertEquals(recordedState.getLinearId(), inputState.getLinearId());
        }
    }
}
