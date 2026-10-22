package com.mari.iso20022.agent.reasoning;

import com.mari.iso20022.agent.evidence.AssessedEvidenceItem;
import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.risk.RiskAssessment;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.validation.ValidationFinding;

import java.util.List;

public record ReasoningAgentRequest(String transactionId,
                                    ReasoningPaymentSummary paymentSummary,
                                    List<ValidationFinding> validationFindings,
                                    HistoricalAnalysisResult historicalAnalysis,
                                    EvidenceAgentResponse evidenceAssessment,
                                    RiskAssessment riskAssessment,
                                    List<AssessedEvidenceItem> evidenceItems) {
    public ReasoningAgentRequest {
        validationFindings = List.copyOf(validationFindings);
        evidenceItems = List.copyOf(evidenceItems);
    }
}
