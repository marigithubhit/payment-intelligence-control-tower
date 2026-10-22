package com.mari.iso20022.investigation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.mari.iso20022.investigation.dto.CollectedEvidence;
import com.mari.iso20022.investigation.dto.EvidenceType;

@Entity
@Table(name = "investigation_evidence")
public class InvestigationEvidence {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investigation_case_id", nullable = false)
    private InvestigationCase investigationCase;

    @Column(name = "evidence_type", nullable = false, length = 64)
    private String evidenceType;
    @Column(length = 64)
    private String category;
    @Column(name = "rule_code", length = 64)
    private String ruleCode;
    @Column(length = 32)
    private String severity;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(name = "source_reference", nullable = false, length = 255)
    private String sourceReference;
    @Column(name = "observed_value", length = 500)
    private String observedValue;
    @Column(name = "expected_value", length = 500)
    private String expectedValue;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected InvestigationEvidence() {
    }

    public static InvestigationEvidence validationFinding(String category, String ruleCode, String severity,
                                                          String description, String sourceReference) {
        InvestigationEvidence evidence = new InvestigationEvidence();
        evidence.id = UUID.randomUUID();
        evidence.evidenceType = EvidenceType.VALIDATION_FINDING.name();
        evidence.category = category;
        evidence.ruleCode = ruleCode;
        evidence.severity = severity;
        evidence.description = description;
        evidence.sourceReference = sourceReference;
        return evidence;
    }

    public static InvestigationEvidence from(CollectedEvidence collected) {
        InvestigationEvidence evidence = new InvestigationEvidence();
        evidence.id = UUID.randomUUID();
        evidence.evidenceType = collected.evidenceType().name();
        evidence.category = collected.category();
        evidence.ruleCode = collected.ruleCode();
        evidence.severity = collected.severity();
        evidence.description = collected.description();
        evidence.sourceReference = collected.sourceReference();
        evidence.observedValue = collected.observedValue();
        evidence.expectedValue = collected.expectedValue();
        return evidence;
    }

    void setInvestigationCase(InvestigationCase investigationCase) {
        this.investigationCase = investigationCase;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getEvidenceType() { return evidenceType; }
    public String getCategory() { return category; }
    public String getRuleCode() { return ruleCode; }
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
    public String getSourceReference() { return sourceReference; }
    public String getObservedValue() { return observedValue; }
    public String getExpectedValue() { return expectedValue; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
