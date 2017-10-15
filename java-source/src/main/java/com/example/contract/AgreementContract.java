package com.example.contract;

import com.example.state.AgreementState;
import net.corda.core.contracts.AuthenticatedObject;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TransactionForContract;
import net.corda.core.crypto.SecureHash;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class AgreementContract  implements Contract {

    @Override
    public void verify(TransactionForContract tx) {
        System.out.println("AgreementContract verify "+tx.getOutputs().get(0));
        final AuthenticatedObject<AgreementContract.Commands.Create> command = requireSingleCommand(tx.getCommands(), AgreementContract.Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an IOU.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final AgreementState out = (AgreementState) tx.getOutputs().get(0);
            require.using("The sender and the recipient cannot be the same entity.",
                    out.getSender() != out.getRecipient());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants()));

            // IOU-specific constraints.
            require.using("The IOU's value must be non-negative.",
                    out.getAgreement().getAddress() != "");
            require.using("The IOU's name must be present.",
                    out.getAgreement().getName() != "");
            require.using("The IOU's price must be present.",
                    out.getAgreement().getEligibleCollateral() != "");

            return null;
        });
    }

    /**
     * This contract only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements AgreementContract.Commands {}
    }

    /** This is a reference to the underlying legal contract template and associated parameters. */
    private final SecureHash legalContractReference = SecureHash.sha256("Agreement contract template and params");
    @Override public final SecureHash getLegalContractReference() { return legalContractReference; }
}
