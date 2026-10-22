package com.mari.iso20022.agent.risk;

public record RiskAgentResponse(String transactionId, RiskAssessment assessment,
                                long executionDurationMillis) {
}
