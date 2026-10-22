package com.mari.iso20022.agent.evidence;

import java.math.BigDecimal;
import java.util.List;

public record EvidenceAgentResponse(String transactionId,
                                    List<AssessedEvidenceItem> evidenceItems,
                                    EvidenceQuality evidenceQuality,
                                    EvidenceCompleteness evidenceCompleteness,
                                    BigDecimal evidenceStrengthScore,
                                    List<String> missingEvidence,
                                    boolean conflictingEvidence,
                                    String summary,
                                    long executionDurationMillis) {
    public EvidenceAgentResponse {
        evidenceItems = List.copyOf(evidenceItems);
        missingEvidence = List.copyOf(missingEvidence);
    }
}
