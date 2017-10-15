package com.example.client;

import com.example.state.AgreementState;
import com.google.common.net.HostAndPort;
import kotlin.Pair;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class CordaClientRPC {
    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: CordaClientRPC <node address>");
        }

        final Logger logger = LoggerFactory.getLogger(CordaClientRPC.class);
        final HostAndPort nodeAddress = HostAndPort.fromString(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, null, null);

        // Can be amended in the com.example.Main file.
        client.start("user1", "test");
        final CordaRPCOps proxy = client.proxy();

        // Grab all signed transactions and all future signed transactions.
        final Pair<List<SignedTransaction>, Observable<SignedTransaction>> txsAndFutureTxs =
                proxy.verifiedTransactions();
        final List<SignedTransaction> txs = txsAndFutureTxs.getFirst();
        final Observable<SignedTransaction> futureTxs = txsAndFutureTxs.getSecond();

        // Log the 'placed' IOUs and listen for new ones.
        futureTxs.startWith(txs).toBlocking().subscribe(
                transaction ->
                        transaction.getTx().getOutputs().forEach(
                                output -> {
                                    final AgreementState agreementState = (AgreementState) output.getData();
                                    logger.info(agreementState.getAgreement().toString());
                                })
        );
    }
}
