package com.mari.iso20022.investigation.dto;

import com.mari.iso20022.investigation.entity.InvestigationCase;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InvestigationCaseResponse(UUID caseId,
                                        String transactionId,
                                        String status,
                                        String riskLevel,
                                        boolean humanReviewRequired,
                                        String recommendation,
                                        String summary,
                                        OffsetDateTime openedAt,
                                        OffsetDateTime completedAt,
                                        OffsetDateTime createdAt,
                                        OffsetDateTime updatedAt,
                                        List<InvestigationEvidenceResponse> evidence) {
    public InvestigationCaseResponse {
        evidence = List.copyOf(evidence);
    }

    public static InvestigationCaseResponse from(InvestigationCase investigationCase,
                                                  List<InvestigationEvidenceResponse> evidence) {
        return new InvestigationCaseResponse(investigationCase.getId(), investigationCase.getTransactionId(),
                investigationCase.getStatus(), investigationCase.getRiskLevel(), investigationCase.isHumanReviewRequired(),
                investigationCase.getRecommendation(),
                investigationCase.getSummary(), investigationCase.getOpenedAt(), investigationCase.getCompletedAt(),
                investigationCase.getCreatedAt(), investigationCase.getUpdatedAt(), evidence);
    }
}
