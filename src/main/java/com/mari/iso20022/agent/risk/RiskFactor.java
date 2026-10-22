package com.mari.iso20022.agent.risk;

import com.mari.iso20022.validation.ValidationSeverity;

import java.util.List;

public record RiskFactor(String code, ValidationSeverity severity, int points,
                         String description, List<String> evidenceReferences) {
    public RiskFactor {
        evidenceReferences = List.copyOf(evidenceReferences);
    }
}
