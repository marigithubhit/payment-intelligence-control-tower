package com.mari.iso20022.agent.risk;

import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.evidence.EvidenceCompleteness;
import com.mari.iso20022.agent.evidence.EvidenceQuality;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.HistoricalTransactionSummary;
import com.mari.iso20022.investigation.dto.RiskLevel;
import com.mari.iso20022.validation.ValidationCategory;
import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.validation.ValidationSeverity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicRiskAgentTest {
    private final RiskAgentProperties properties = new RiskAgentProperties();
    private final DeterministicRiskAgent agent = new DeterministicRiskAgent(properties);

    @Test
    void zeroFindingsProduceLowNoActionWithoutChangingEvidenceConfidence() {
        var result = agent.assess(request(List.of(), evidence(EvidenceQuality.STRONG, EvidenceCompleteness.COMPLETE,
                new BigDecimal("0.92"), false)));
        assertEquals(0, result.assessment().riskScore());
        assertEquals(RiskLevel.LOW, result.assessment().riskLevel());
        assertEquals(new BigDecimal("0.92"), result.assessment().confidence());
        assertFalse(result.assessment().requiresHumanReview());
        assertEquals("NO_ACTION", result.assessment().recommendedAction());
    }

    @Test
    void severityPointsMapToExpectedRiskLevelsAndActions() {
        assertScore(ValidationSeverity.LOW, 10, RiskLevel.LOW, "MONITOR");
        assertScore(ValidationSeverity.MEDIUM, 25, RiskLevel.MEDIUM, "REVIEW");
        assertScore(ValidationSeverity.HIGH, 50, RiskLevel.HIGH, "MANUAL_REVIEW");
        assertScore(ValidationSeverity.CRITICAL, 80, RiskLevel.CRITICAL, "HOLD_AND_ESCALATE");
    }

    @Test
    void aggregatesMultipleFindingsAndCapsAtOneHundred() {
        var result = agent.assess(request(List.of(
                finding("HIGH_ONE", ValidationSeverity.CRITICAL),
                finding("HIGH_TWO", ValidationSeverity.CRITICAL),
                finding("HIGH_THREE", ValidationSeverity.MEDIUM)),
                evidence(EvidenceQuality.STRONG, EvidenceCompleteness.COMPLETE, new BigDecimal("0.95"), false)));
        assertEquals(100, result.assessment().riskScore());
        assertEquals(3, result.assessment().riskFactors().size());
        assertTrue(result.assessment().riskFactors().stream().allMatch(factor -> factor.points() > 0));
        assertEquals(RiskLevel.CRITICAL, result.assessment().riskLevel());
    }

    @Test
    void lowConfidenceInsufficientEvidenceAndConflictsRequireReview() {
        var lowConfidence = agent.assess(request(List.of(), evidence(EvidenceQuality.WEAK,
                EvidenceCompleteness.PARTIAL, new BigDecimal("0.40"), false))).assessment();
        assertTrue(lowConfidence.requiresHumanReview());
        assertEquals("REVIEW", lowConfidence.recommendedAction());

        var insufficient = agent.assess(request(List.of(), evidence(EvidenceQuality.INSUFFICIENT,
                EvidenceCompleteness.INSUFFICIENT, BigDecimal.ZERO, false))).assessment();
        assertTrue(insufficient.requiresHumanReview());

        var conflict = agent.assess(request(List.of(), evidence(EvidenceQuality.STRONG,
                EvidenceCompleteness.COMPLETE, new BigDecimal("0.95"), true))).assessment();
        assertTrue(conflict.requiresHumanReview());
        assertEquals(new BigDecimal("0.75"), conflict.confidence());
    }

    private void assertScore(ValidationSeverity severity, int score, RiskLevel level, String action) {
        var finding = finding("RULE_" + severity, severity);
        var assessment = agent.assess(request(List.of(finding), evidence(EvidenceQuality.STRONG,
                EvidenceCompleteness.COMPLETE, new BigDecimal("0.95"), false))).assessment();
        assertEquals(score, assessment.riskScore());
        assertEquals(level, assessment.riskLevel());
        assertEquals(action, assessment.recommendedAction());
        assertEquals(finding.ruleCode(), assessment.riskFactors().getFirst().code());
        assertTrue(assessment.riskFactors().getFirst().description().contains(finding.evidence()));
    }

    static EvidenceAgentResponse evidence(EvidenceQuality quality, EvidenceCompleteness completeness,
                                          BigDecimal score, boolean conflict) {
        return new EvidenceAgentResponse("TXN1", List.of(), quality, completeness, score, List.of(),
                conflict, "Evidence summary", 0);
    }

    static RiskAgentRequest request(List<com.mari.iso20022.validation.ValidationFinding> findings,
                                    EvidenceAgentResponse evidence) {
        var summary = new HistoricalTransactionSummary(0, 0, 0, 0, null, null, null, null);
        var history = new HistoricalAnalysisResult("TXN1", 90, null, null, summary, new BigDecimal("500.00"),
                "AED", null, List.of(), 0, 0, 0, null, null, List.of());
        return new RiskAgentRequest("TXN1", new BigDecimal("500"), "AED", findings, evidence, history);
    }

    private static ValidationFinding finding(String code, ValidationSeverity severity) {
        return ValidationFinding.failed(code, severity, ValidationCategory.BENEFICIARY,
                "Observed validation finding", "creditorIban", "Evidence from persisted records");
    }
}
