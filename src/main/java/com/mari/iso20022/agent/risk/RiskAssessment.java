package com.mari.iso20022.agent.risk;

import com.mari.iso20022.investigation.dto.RiskLevel;

import java.math.BigDecimal;
import java.util.List;

public record RiskAssessment(String transactionId, int riskScore, RiskLevel riskLevel,
                             BigDecimal confidence, List<RiskFactor> riskFactors,
                             String recommendedAction, boolean requiresHumanReview,
                             String explanation) {
    public RiskAssessment {
        riskFactors = List.copyOf(riskFactors);
    }
}
