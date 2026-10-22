package com.mari.iso20022.validation;

public record ValidationFinding(
        String ruleCode,
        ValidationSeverity severity,
        ValidationCategory category,
        String message,
        String field,
        String evidence,
        boolean passed) {

    public static ValidationFinding failed(String code, ValidationSeverity severity, ValidationCategory category,
                                           String message, String field, String evidence) {
        return new ValidationFinding(code, severity, category, message, field, evidence, false);
    }
}
