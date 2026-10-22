package com.mari.iso20022.agent.risk;

import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.validation.ValidationFinding;

import java.math.BigDecimal;
import java.util.List;

public record RiskAgentRequest(String transactionId, BigDecimal amount, String currency,
                               List<ValidationFinding> validationFindings,
                               EvidenceAgentResponse evidenceAssessment,
                               HistoricalAnalysisResult historicalAnalysis) {
    public RiskAgentRequest {
        validationFindings = List.copyOf(validationFindings);
    }
}
