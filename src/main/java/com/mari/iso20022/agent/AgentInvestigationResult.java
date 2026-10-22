package com.mari.iso20022.agent;

import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.risk.RiskAgentResponse;
import com.mari.iso20022.agent.reasoning.ReasoningAgentResponse;
import com.mari.iso20022.agent.verification.VerificationAgentResponse;

public record AgentInvestigationResult(String transactionId,
                                       EvidenceAgentResponse evidenceAgent,
                                       RiskAgentResponse riskAgent,
                                       ReasoningAgentResponse reasoningAgent,
                                       VerificationAgentResponse verificationAgent) {
    public AgentInvestigationResult(String transactionId, EvidenceAgentResponse evidenceAgent,
                                    RiskAgentResponse riskAgent) {
        this(transactionId, evidenceAgent, riskAgent, null, null);
    }
    public AgentInvestigationResult(String transactionId, EvidenceAgentResponse evidenceAgent,
                                    RiskAgentResponse riskAgent, ReasoningAgentResponse reasoningAgent) {
        this(transactionId, evidenceAgent, riskAgent, reasoningAgent, null);
    }
}
