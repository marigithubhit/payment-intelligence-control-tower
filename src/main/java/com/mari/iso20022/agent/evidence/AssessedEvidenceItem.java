package com.mari.iso20022.agent.evidence;

public record AssessedEvidenceItem(EvidenceClassification classification, String evidenceType,
                                   String ruleCode, String sourceReference, String description,
                                   String observedValue, String expectedValue) {
}
