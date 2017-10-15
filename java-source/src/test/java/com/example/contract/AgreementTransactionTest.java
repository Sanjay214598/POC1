package com.example.contract;

import com.example.model.Agreement;
import com.example.state.AgreementState;
import net.corda.core.crypto.Party;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.PublicKey;

import static net.corda.testing.CoreTestUtils.*;

public class AgreementTransactionTest {
    static private final Party miniCorp = getMINI_CORP();
    static private final Party megaCorp = getMEGA_CORP();
    static private final PublicKey[] keys = new PublicKey[2];

    @BeforeClass
    public static void setUpClass() {
        keys[0] = getMEGA_CORP_PUBKEY();
        keys[1] = getMINI_CORP_PUBKEY();
    }

    @Test
    public void transactionMustIncludeCreateCommand() {
        Agreement agreement = new Agreement("A", "A","a", "A", "A","a", "A", "A","a");
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                txDSL.fails();
                txDSL.command(keys, AgreementContract.Commands.Create::new);
                txDSL.verifies();
                return null;
            });
            return null;
        });
    }

    /*@Test
    public void transactionMustHaveNoInputs() {
        Agreement agreement = new Agreement("A", "A","a", "A", "A","a", "A", "A","a");
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.input(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                txDSL.output(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                txDSL.command(keys, AgreementContract.Commands.Create::new);
                txDSL.failsWith("No inputs should be consumed when issuing an Agreement.");
                return null;
            });
            return null;
        });
    }*/

    @Test
    public void transactionMustHaveOneOutput() {
        Agreement agreement = new Agreement("A", "A","a", "A", "A","a", "A", "A","a");
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                txDSL.output(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                txDSL.command(keys, AgreementContract.Commands.Create::new);
                txDSL.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void senderMustSignTransaction() {
        Agreement agreement = new Agreement("A", "A","a", "A", "A","a", "A", "A","a");
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, AgreementContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void recipientMustSignTransaction() {
        Agreement agreement = new Agreement("A", "A","a", "A", "A","a", "A", "A","a");
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, AgreementContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void senderIsNotRecipient() {
        Agreement agreement = new Agreement("A", "A","a", "A", "A","a", "A", "A","a");
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new AgreementState(agreement, megaCorp, megaCorp, new AgreementContract()));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, AgreementContract.Commands.Create::new);
                txDSL.failsWith("The sender and the recipient cannot be the same entity.");
                return null;
            });
            return null;
        });
    }

    /*@Test
    public void cannotCreateNegativeValueAgreements() {
        Agreement agreement = new Agreement("A", "A","a", "A", "A","a", "A", "A","a");
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new AgreementState(agreement, miniCorp, megaCorp, new AgreementContract()));
                txDSL.command(keys, AgreementContract.Commands.Create::new);
                txDSL.failsWith("The Agreement's value must be non-negative.");
                return null;
            });
            return null;
        });
    }*/
}
