package com.mari.iso20022.investigation.dto;

import com.mari.iso20022.investigation.entity.InvestigationReview;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvestigationReviewResponse(UUID reviewId, UUID caseId, String transactionId,
                                          ReviewDecision decision, String reason, String reviewer,
                                          String previousStatus, String newStatus,
                                          OffsetDateTime reviewedAt) {
    public static InvestigationReviewResponse from(InvestigationReview review) {
        return new InvestigationReviewResponse(review.getId(), review.getInvestigationCase().getId(),
                review.getInvestigationCase().getTransactionId(), review.getDecision(), review.getReason(),
                review.getReviewer(), review.getPreviousStatus(), review.getNewStatus(), review.getReviewedAt());
    }
}
