package com.mari.iso20022.investigation;

import com.mari.iso20022.investigation.dto.InvestigationStatus;
import com.mari.iso20022.investigation.dto.RiskLevel;
import com.mari.iso20022.validation.ValidationCategory;
import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.validation.ValidationSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskAssessmentServiceTest {
    private final RiskAssessmentService service = new RiskAssessmentService();

    @Test
    void mapsRiskAndRecommendationsDeterministically() {
        assertEquals(RiskLevel.LOW, service.assess(List.of()));
        assertEquals(RiskLevel.MEDIUM, service.assess(List.of(finding(ValidationSeverity.MEDIUM))));
        assertEquals(RiskLevel.HIGH, service.assess(List.of(finding(ValidationSeverity.HIGH))));
        assertEquals(RiskLevel.CRITICAL, service.assess(List.of(
                finding(ValidationSeverity.HIGH), finding(ValidationSeverity.HIGH))));
        assertEquals(RiskLevel.CRITICAL, service.assess(List.of(finding(ValidationSeverity.CRITICAL))));
        assertEquals("MANUAL_REVIEW", service.recommendation(InvestigationStatus.REQUIRES_REVIEW, RiskLevel.HIGH));
        assertEquals("HOLD_AND_ESCALATE", service.recommendation(InvestigationStatus.REQUIRES_REVIEW, RiskLevel.CRITICAL));
    }

    private ValidationFinding finding(ValidationSeverity severity) {
        return ValidationFinding.failed("TEST_RULE", severity, ValidationCategory.AMOUNT,
                "synthetic test", "amount", "test evidence");
    }
}
