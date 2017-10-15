package com.example.api;

import com.example.contract.AgreementContract;
import com.example.model.Agreement;
import com.example.state.AgreementState;
import com.example.flow.AgreementFlow;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import kotlin.Pair;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.NetworkMapCache;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static net.corda.client.rpc.UtilsKt.notUsed;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("counterparty")
public class CounterPartyController {

    private final CordaRPCOps services;
    private final String myLegalName;
    private final List<String> notaryNames = Lists.newArrayList("Controller", "NetworkMapService");

    static private final Logger logger = LoggerFactory.getLogger(CounterPartyController.class);

    public CounterPartyController(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeIdentity().getLegalIdentity().getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> whoami() { return ImmutableMap.of("me", myLegalName); }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getPeers() {
        Pair<List<NodeInfo>, Observable<NetworkMapCache.MapChange>> nodeInfo = services.networkMapUpdates();
        notUsed(nodeInfo.getSecond());
        System.out.println("nodeInfo.getSecond() "+nodeInfo.getSecond());
        return ImmutableMap.of(
                "peers",
                nodeInfo.getFirst()
                        .stream()
                        .map(node -> node.getLegalIdentity().getName())
                        .filter(name -> !name.equals(myLegalName) && !(notaryNames.contains(name)))
                        .collect(toList()));
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("contracts/{cpName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getContracts(@PathParam("cpName") String cpName) {
        List<StateAndRef<ContractState>> retList = new ArrayList<StateAndRef<ContractState>>();
        System.out.println("CP Name: "+cpName);
        Pair<List<StateAndRef<ContractState>>, Observable<Vault.Update>> vaultUpdates = services.vaultAndUpdates();
        notUsed(vaultUpdates.getSecond());
        for (int i=0; i < vaultUpdates.getFirst().size();i++) {
            AgreementState state = (AgreementState)vaultUpdates.getFirst().get(i).component1().getData();
            System.out.println("state.getSender().getName().equalsIgnoreCase(cpName) "+state.getSender().getName()+":::"+cpName);
            if(state.getSender().getName().equalsIgnoreCase(cpName))
            {
                retList.add(vaultUpdates.getFirst().get(i));
            }

            System.out.println("111 "+vaultUpdates.getFirst().get(i).toString());
            System.out.println("qqq "+((AgreementState)vaultUpdates.getFirst().get(i).component1().getData()).getRecipient().getName());
            System.out.println("qqq333 "+((AgreementState)vaultUpdates.getFirst().get(i).component1().getData()).getSender().getName());
            System.out.println("qqq1 "+vaultUpdates.getFirst().get(i).component2().toString());
            System.out.println("qqq11 "+vaultUpdates.getFirst().get(i).component1().getNotary().toString());
        }
        //return vaultUpdates.getFirst();
        return retList;
    }

    @GET
    @Path("mycontracts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getMyContracts() {
        
        Pair<List<StateAndRef<ContractState>>, Observable<Vault.Update>> vaultUpdates = services.vaultAndUpdates();
        notUsed(vaultUpdates.getSecond());
        for (int i=0; i < vaultUpdates.getFirst().size();i++) {
            System.out.println("111 "+vaultUpdates.getFirst().get(i).toString());
            System.out.println("qqq "+vaultUpdates.getFirst().get(i).component1().toString());
        }
        return vaultUpdates.getFirst();
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the sender and the recipient will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("{party}/create-iou")
    public Response createIOU(Agreement agreement, @PathParam("party") String partyName) throws InterruptedException, ExecutionException {
        final Party otherParty = services.partyFromName(partyName);
        System.out.println("REached here "+partyName);
        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        System.out.println("otherParty.getName() "+otherParty.getName());
        System.out.println("services.nodeIdentity().getLegalIdentity().getName() "+services.nodeIdentity().getLegalIdentity().getName());
        final AgreementState state = new AgreementState(
                agreement,
                services.nodeIdentity().getLegalIdentity(),
                otherParty,
                new AgreementContract());

        Response.Status status;
        String msg;
        try {
            FlowProgressHandle<SignedTransaction> flowHandle = services
                    .startTrackedFlowDynamic(AgreementFlow.Initiator.class, state, otherParty);
            System.out.println("Strting the flow: "+otherParty.getName());
            flowHandle.getProgress().subscribe(evt -> System.out.printf("flow initiated::: >> %s\n", evt));

            // The line below blocks and waits for the flow to return.
            final SignedTransaction result = flowHandle
                    .getReturnValue()
                    .get();

            status = Response.Status.CREATED;
            msg = String.format("Transaction id %s committed to ledger.", result.getId());

        } catch (Throwable ex) {
            status = Response.Status.BAD_REQUEST;
            msg = "Transaction failed.";
            logger.error(ex.getMessage(), ex);
        }

        return Response
                .status(status)
                .entity(msg)
                .build();
    }
}
