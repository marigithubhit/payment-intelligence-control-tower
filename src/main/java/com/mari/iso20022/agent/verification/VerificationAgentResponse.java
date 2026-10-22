package com.mari.iso20022.agent.verification;

import com.mari.iso20022.investigation.dto.RiskLevel;

import java.util.List;

public record VerificationAgentResponse(String transactionId,
                                        VerificationStatus verificationStatus,
                                        int verificationScore,
                                        List<VerificationFinding> findings,
                                        List<String> verifiedClaims,
                                        List<String> unsupportedClaims,
                                        List<String> verifiedEvidenceReferences,
                                        boolean recommendationVerified,
                                        RiskLevel riskLevelPreserved,
                                        boolean requiresHumanReview) {
    public VerificationAgentResponse {
        findings = List.copyOf(findings);
        verifiedClaims = List.copyOf(verifiedClaims);
        unsupportedClaims = List.copyOf(unsupportedClaims);
        verifiedEvidenceReferences = List.copyOf(verifiedEvidenceReferences);
    }

    public static VerificationAgentResponse from(String transactionId, VerificationResult result) {
        return new VerificationAgentResponse(transactionId, result.verificationStatus(), result.verificationScore(),
                result.findings(), result.verifiedClaims(), result.unsupportedClaims(),
                result.verifiedEvidenceReferences(), result.recommendationVerified(), result.riskLevelPreserved(),
                result.requiresHumanReview());
    }
}
