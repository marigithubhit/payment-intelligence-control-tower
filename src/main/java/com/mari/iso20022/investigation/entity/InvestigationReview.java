package com.mari.iso20022.investigation.entity;

import com.mari.iso20022.investigation.dto.ReviewDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "investigation_review")
public class InvestigationReview {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investigation_case_id", nullable = false)
    private InvestigationCase investigationCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewDecision decision;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false, length = 100)
    private String reviewer;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @Column(name = "previous_status", nullable = false, length = 32)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    private String newStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected InvestigationReview() {
    }

    public static InvestigationReview record(InvestigationCase investigationCase, ReviewDecision decision,
                                             String reason, String reviewer, String previousStatus,
                                             String newStatus) {
        InvestigationReview review = new InvestigationReview();
        review.id = UUID.randomUUID();
        review.investigationCase = investigationCase;
        review.decision = decision;
        review.reason = reason;
        review.reviewer = reviewer;
        review.reviewedAt = OffsetDateTime.now();
        review.previousStatus = previousStatus;
        review.newStatus = newStatus;
        return review;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public InvestigationCase getInvestigationCase() { return investigationCase; }
    public ReviewDecision getDecision() { return decision; }
    public String getReason() { return reason; }
    public String getReviewer() { return reviewer; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
