package com.mari.iso20022.agent.risk;

public interface RiskAgent {
    RiskAgentResponse assess(RiskAgentRequest request);
}
