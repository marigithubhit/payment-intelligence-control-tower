package com.mari.iso20022.agent.reasoning;

import java.math.BigDecimal;
import java.util.List;

public record ReasoningResult(String transactionId,
                              String summary,
                              List<ObservedFact> observedFacts,
                              List<EvidenceAssessmentItem> evidenceAssessment,
                              List<ReasoningStatement> reasoning,
                              List<String> uncertainties,
                              List<String> summaryEvidenceIds,
                              AnalystAction recommendedAnalystAction,
                              BigDecimal confidence) {
    public ReasoningResult {
        observedFacts = observedFacts == null ? null : List.copyOf(observedFacts);
        evidenceAssessment = evidenceAssessment == null ? null : List.copyOf(evidenceAssessment);
        reasoning = reasoning == null ? null : List.copyOf(reasoning);
        uncertainties = uncertainties == null ? null : List.copyOf(uncertainties);
        summaryEvidenceIds = summaryEvidenceIds == null ? null : List.copyOf(summaryEvidenceIds);
    }

    public record ObservedFact(String evidenceId, String fact) { }
    public record EvidenceAssessmentItem(String assessment, EvidenceImportance importance,
                                         List<String> evidenceIds) {
        public EvidenceAssessmentItem {
            evidenceIds = evidenceIds == null ? null : List.copyOf(evidenceIds);
        }
    }
    public record ReasoningStatement(String statement, List<String> evidenceIds) {
        public ReasoningStatement {
            evidenceIds = evidenceIds == null ? null : List.copyOf(evidenceIds);
        }
    }
}
