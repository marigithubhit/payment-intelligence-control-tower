package com.mari.iso20022.agent.reasoning;

public record ReasoningEvidenceReference(String evidenceId, String evidenceType, String ruleCode,
                                        String sourceReference, String fact) { }
