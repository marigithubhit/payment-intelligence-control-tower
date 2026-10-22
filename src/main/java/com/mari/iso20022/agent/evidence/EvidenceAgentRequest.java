package com.mari.iso20022.agent.evidence;

import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.InvestigationEvidenceResponse;
import com.mari.iso20022.validation.ValidationFinding;

import java.util.List;

public record EvidenceAgentRequest(AgentPaymentSummary payment,
                                  List<ValidationFinding> validationFindings,
                                  HistoricalAnalysisResult historicalAnalysis,
                                  List<InvestigationEvidenceResponse> investigationEvidence) {
    public EvidenceAgentRequest {
        validationFindings = List.copyOf(validationFindings);
        investigationEvidence = List.copyOf(investigationEvidence);
    }
}
