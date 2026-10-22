package com.mari.iso20022.agent.verification;

public interface VerificationAgent {
    VerificationAgentResponse verify(VerificationAgentRequest request);
}
