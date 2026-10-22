package com.mari.iso20022.agent.reasoning;

import java.util.Map;
import java.util.Set;

public record ReasoningPrompt(String systemMessage, String userMessage, Map<String, Object> jsonSchema,
                              Map<String, String> allowedFacts, Set<String> allowedEvidenceIds,
                              Set<String> allowedUncertainties) {
    public ReasoningPrompt {
        allowedFacts = Map.copyOf(allowedFacts);
        allowedEvidenceIds = Set.copyOf(allowedEvidenceIds);
        allowedUncertainties = Set.copyOf(allowedUncertainties);
    }
}
