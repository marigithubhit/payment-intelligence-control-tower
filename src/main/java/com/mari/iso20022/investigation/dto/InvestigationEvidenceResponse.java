package com.mari.iso20022.investigation.dto;

import com.mari.iso20022.investigation.entity.InvestigationEvidence;

import java.time.OffsetDateTime;

public record InvestigationEvidenceResponse(String evidenceType,
                                            String category,
                                            String ruleCode,
                                            String severity,
                                            String description,
                                            String sourceReference,
                                            String observedValue,
                                            String expectedValue,
                                            OffsetDateTime createdAt) {
    public static InvestigationEvidenceResponse from(InvestigationEvidence evidence) {
        return new InvestigationEvidenceResponse(evidence.getEvidenceType(), evidence.getCategory(),
                evidence.getRuleCode(), evidence.getSeverity(), evidence.getDescription(),
                evidence.getSourceReference(), evidence.getObservedValue(), evidence.getExpectedValue(),
                evidence.getCreatedAt());
    }
}
