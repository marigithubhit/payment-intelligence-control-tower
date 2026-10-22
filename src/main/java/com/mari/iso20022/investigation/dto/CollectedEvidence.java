package com.mari.iso20022.investigation.dto;

public record CollectedEvidence(EvidenceType evidenceType, String category, String ruleCode,
                                String severity, String description, String sourceReference,
                                String observedValue, String expectedValue) {
}
