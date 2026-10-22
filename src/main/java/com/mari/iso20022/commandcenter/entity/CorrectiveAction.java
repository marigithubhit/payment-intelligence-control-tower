package com.mari.iso20022.commandcenter.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "corrective_action")
public class CorrectiveAction {

    @Id
    private UUID id;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "transaction_id", nullable = false, length = 35)
    private String transactionId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "assigned_team", nullable = false, length = 64)
    private String assignedTeam;

    @Column(name = "assigned_role", nullable = false, length = 64)
    private String assignedRole;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "human_approval_required", nullable = false)
    private boolean humanApprovalRequired;

    @Column(name = "approval_status", nullable = false, length = 32)
    private String approvalStatus;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approval_notes", length = 2000)
    private String approvalNotes;

    @Column(name = "target_metric", length = 100)
    private String targetMetric;

    @Column(name = "baseline_metric_value", length = 64)
    private String baselineMetricValue;

    @Column(name = "post_action_metric_value", length = 64)
    private String postActionMetricValue;

    @Column(name = "outcome_status", nullable = false, length = 32)
    private String outcomeStatus;

    @Column(name = "outcome_notes", length = 2000)
    private String outcomeNotes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<CorrectiveActionNote> notes = new ArrayList<>();

    public CorrectiveAction() {}

    public CorrectiveAction(UUID id, UUID caseId, String transactionId, String title, String description,
                            String actionType, String status, String assignedTeam, String assignedRole,
                            String assignedTo, boolean humanApprovalRequired, String approvalStatus,
                            String targetMetric, String baselineMetricValue, OffsetDateTime createdAt) {
        this.id = id;
        this.caseId = caseId;
        this.transactionId = transactionId;
        this.title = title;
        this.description = description;
        this.actionType = actionType;
        this.status = status;
        this.assignedTeam = assignedTeam;
        this.assignedRole = assignedRole;
        this.assignedTo = assignedTo;
        this.humanApprovalRequired = humanApprovalRequired;
        this.approvalStatus = approvalStatus;
        this.targetMetric = targetMetric;
        this.baselineMetricValue = baselineMetricValue;
        this.outcomeStatus = "PENDING_EVALUATION";
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(String assignedTeam) { this.assignedTeam = assignedTeam; }

    public String getAssignedRole() { return assignedRole; }
    public void setAssignedRole(String assignedRole) { this.assignedRole = assignedRole; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public boolean isHumanApprovalRequired() { return humanApprovalRequired; }
    public void setHumanApprovalRequired(boolean humanApprovalRequired) { this.humanApprovalRequired = humanApprovalRequired; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public String getTargetMetric() { return targetMetric; }
    public void setTargetMetric(String targetMetric) { this.targetMetric = targetMetric; }

    public String getBaselineMetricValue() { return baselineMetricValue; }
    public void setBaselineMetricValue(String baselineMetricValue) { this.baselineMetricValue = baselineMetricValue; }

    public String getPostActionMetricValue() { return postActionMetricValue; }
    public void setPostActionMetricValue(String postActionMetricValue) { this.postActionMetricValue = postActionMetricValue; }

    public String getOutcomeStatus() { return outcomeStatus; }
    public void setOutcomeStatus(String outcomeStatus) { this.outcomeStatus = outcomeStatus; }

    public String getOutcomeNotes() { return outcomeNotes; }
    public void setOutcomeNotes(String outcomeNotes) { this.outcomeNotes = outcomeNotes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public List<CorrectiveActionNote> getNotes() { return notes; }
    public void setNotes(List<CorrectiveActionNote> notes) { this.notes = notes; }

    public void addNote(CorrectiveActionNote note) {
        notes.add(note);
        note.setAction(this);
    }
}
