package com.example.service;

import com.example.flow.AgreementFlow;
import net.corda.core.node.PluginServiceHub;

public class AgreementService {
    public AgreementService(PluginServiceHub services) {
        services.registerFlowInitiator(AgreementFlow.Initiator.class, AgreementFlow.Acceptor::new);
    }
}
