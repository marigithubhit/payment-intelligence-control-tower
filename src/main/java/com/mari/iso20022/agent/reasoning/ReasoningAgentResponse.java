package com.mari.iso20022.agent.reasoning;

import java.math.BigDecimal;
import java.util.List;

public record ReasoningAgentResponse(String transactionId,
                                     ReasoningStatus reasoningStatus,
                                     String summary,
                                     List<ReasoningResult.ObservedFact> observedFacts,
                                     List<ReasoningResult.EvidenceAssessmentItem> evidenceAssessment,
                                     List<ReasoningResult.ReasoningStatement> reasoning,
                                     List<ReasoningEvidenceReference> evidenceReferences,
                                     List<String> uncertainties,
                                     AnalystAction recommendedAnalystAction,
                                     BigDecimal reasoningConfidence,
                                     boolean confidenceCapped,
                                     String model,
                                     String promptVersion,
                                     long executionTimeMs,
                                     boolean deterministicInvestigationAvailable,
                                     String reasoningError) {
    public ReasoningAgentResponse {
        observedFacts = observedFacts == null ? List.of() : List.copyOf(observedFacts);
        evidenceAssessment = evidenceAssessment == null ? List.of() : List.copyOf(evidenceAssessment);
        reasoning = reasoning == null ? List.of() : List.copyOf(reasoning);
        evidenceReferences = evidenceReferences == null ? List.of() : List.copyOf(evidenceReferences);
        uncertainties = uncertainties == null ? List.of() : List.copyOf(uncertainties);
    }

    public static ReasoningAgentResponse unavailable(String transactionId, String model, String promptVersion,
                                                     long durationMs, String error) {
        return fallback(transactionId, model, promptVersion, durationMs, ReasoningStatus.UNAVAILABLE, error);
    }

    public static ReasoningAgentResponse rejected(String transactionId, String model, String promptVersion,
                                                  long durationMs, String error) {
        return fallback(transactionId, model, promptVersion, durationMs, ReasoningStatus.REJECTED, error);
    }

    private static ReasoningAgentResponse fallback(String transactionId, String model, String promptVersion,
                                                   long durationMs, ReasoningStatus status, String error) {
        return new ReasoningAgentResponse(transactionId, status,
                "LLM reasoning is unavailable; deterministic investigation results remain available.",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, null, false, model, promptVersion,
                durationMs, true, error);
    }
}
