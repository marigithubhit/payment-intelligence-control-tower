package com.mari.iso20022.investigation;

import com.mari.iso20022.investigation.dto.InvestigationStatus;
import com.mari.iso20022.investigation.dto.RiskLevel;
import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.validation.ValidationSeverity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskAssessmentService {
    public RiskLevel assess(List<ValidationFinding> findings) {
        long critical = count(findings, ValidationSeverity.CRITICAL);
        long high = count(findings, ValidationSeverity.HIGH);
        long medium = count(findings, ValidationSeverity.MEDIUM);
        if (critical > 0 || high >= 2) return RiskLevel.CRITICAL;
        if (high > 0 || medium >= 2) return RiskLevel.HIGH;
        if (medium > 0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public InvestigationStatus status(List<ValidationFinding> findings) {
        return findings.stream().anyMatch(finding -> finding.severity().ordinal() >= ValidationSeverity.MEDIUM.ordinal())
                ? InvestigationStatus.REQUIRES_REVIEW : InvestigationStatus.CLEAR;
    }

    public String recommendation(InvestigationStatus status, RiskLevel riskLevel) {
        if (status == InvestigationStatus.CLEAR) return "NO_ACTION";
        return switch (riskLevel) {
            case LOW -> "MONITOR";
            case MEDIUM -> "REVIEW";
            case HIGH -> "MANUAL_REVIEW";
            case CRITICAL -> "HOLD_AND_ESCALATE";
        };
    }

    private long count(List<ValidationFinding> findings, ValidationSeverity severity) {
        return findings.stream().filter(finding -> finding.severity() == severity).count();
    }
}
