package com.mari.iso20022.agent.verification;

import com.mari.iso20022.agent.evidence.AssessedEvidenceItem;
import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.reasoning.ReasoningAgentResponse;
import com.mari.iso20022.agent.reasoning.ReasoningPaymentSummary;
import com.mari.iso20022.agent.risk.RiskAssessment;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.validation.ValidationFinding;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VerificationAgentRequest(@NotBlank String transactionId,
                                      @NotNull @Valid ReasoningAgentResponse reasoningResponse,
                                      @NotNull List<@Valid AssessedEvidenceItem> evidenceItems,
                                      @NotNull @Valid EvidenceAgentResponse evidenceAssessment,
                                      @NotNull @Valid RiskAssessment riskAssessment,
                                      @NotNull List<@Valid ValidationFinding> validationFindings,
                                      @NotNull @Valid HistoricalAnalysisResult historicalAnalysis,
                                      @Valid ReasoningPaymentSummary paymentSummary) {
    public VerificationAgentRequest {
        if (evidenceItems != null) evidenceItems = List.copyOf(evidenceItems);
        if (validationFindings != null) validationFindings = List.copyOf(validationFindings);
    }
}
