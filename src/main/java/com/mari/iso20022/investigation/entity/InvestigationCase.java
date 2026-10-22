package com.mari.iso20022.investigation.entity;

import com.mari.iso20022.investigation.dto.InvestigationResult;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "investigation_case")
public class InvestigationCase {
    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 35)
    private String transactionId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "risk_level", nullable = false, length = 32)
    private String riskLevel;
    @Column(name = "human_review_required", nullable = false)
    private boolean humanReviewRequired;
    @Column(nullable = false, length = 64)
    private String recommendation;
    @Column(nullable = false, length = 1000)
    private String summary;
    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "investigationCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvestigationEvidence> evidence = new ArrayList<>();

    protected InvestigationCase() {
    }

    public static InvestigationCase from(InvestigationResult result, String summary) {
        InvestigationCase investigationCase = new InvestigationCase();
        investigationCase.id = UUID.randomUUID();
        investigationCase.transactionId = result.transactionId();
        investigationCase.status = result.status().name();
        investigationCase.riskLevel = result.riskLevel().name();
        investigationCase.recommendation = result.recommendation();
        investigationCase.summary = summary;
        investigationCase.openedAt = OffsetDateTime.now();
        return investigationCase;
    }

    public void addEvidence(InvestigationEvidence record) {
        evidence.add(record);
        record.setInvestigationCase(this);
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (completedAt == null) completedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
    public String getRiskLevel() { return riskLevel; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public String getRecommendation() { return recommendation; }
    public String getSummary() { return summary; }
    public OffsetDateTime getOpenedAt() { return openedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<InvestigationEvidence> getEvidence() { return List.copyOf(evidence); }

    public void updateWorkflowStatus(String status) {
        this.status = status;
    }

    public void markHumanReviewRequired() {
        this.humanReviewRequired = true;
    }
}
