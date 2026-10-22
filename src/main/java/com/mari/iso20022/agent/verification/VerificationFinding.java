package com.mari.iso20022.agent.verification;

import java.util.List;

public record VerificationFinding(String code, String severity, String message, List<String> evidenceIds) {
    public VerificationFinding {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }
}
