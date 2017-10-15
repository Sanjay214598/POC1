package com.example.state;

import com.example.contract.AgreementContract;
import com.example.model.Agreement;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.Party;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static net.corda.core.crypto.CryptoUtils.getKeys;

public class AgreementState implements LinearState {
    private final Agreement agreement;
    private final Party sender;
    private final Party recipient;
    private final AgreementContract contract;
    private final UniqueIdentifier linearId;

    public AgreementState(Agreement _agreement,
                    Party sender,
                    Party recipient,
                    AgreementContract contract)
    {
        this.agreement = _agreement;
        this.sender = sender;
        this.recipient = recipient;
        this.contract = contract;
        this.linearId = new UniqueIdentifier();
    }

    public Agreement getAgreement() { return agreement; }
    public Party getSender() { return sender; }
    public Party getRecipient() { return recipient; }
    @Override public AgreementContract getContract() { return contract; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<PublicKey> getParticipants() {
        System.out.println("State getParticipants "+sender.getName());
        return Stream.of(sender, recipient)
                .map(Party::getOwningKey)
                .collect(toList());
    }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        final List<PublicKey> partyKeys = Stream.of(sender, recipient)
                .flatMap(party -> getKeys(party.getOwningKey()).stream())
                .collect(toList());
        System.out.println("State isRelevant "+sender.getName());
        return ourKeys
                .stream()
                .anyMatch(partyKeys::contains);

    }
}
