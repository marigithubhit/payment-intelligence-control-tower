package com.mari.iso20022.agent.verification;

import com.mari.iso20022.investigation.dto.RiskLevel;

import java.util.List;

/** The score is a deterministic evidence-trust score, not a probability of fraud. */
public record VerificationResult(VerificationStatus verificationStatus,
                                int verificationScore,
                                List<VerificationFinding> findings,
                                List<String> verifiedClaims,
                                List<String> unsupportedClaims,
                                List<String> verifiedEvidenceReferences,
                                boolean recommendationVerified,
                                RiskLevel riskLevelPreserved,
                                boolean requiresHumanReview) {
    public VerificationResult {
        findings = List.copyOf(findings);
        verifiedClaims = List.copyOf(verifiedClaims);
        unsupportedClaims = List.copyOf(unsupportedClaims);
        verifiedEvidenceReferences = List.copyOf(verifiedEvidenceReferences);
    }
}
