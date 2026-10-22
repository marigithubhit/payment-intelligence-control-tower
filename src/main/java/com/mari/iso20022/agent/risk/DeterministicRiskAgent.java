package com.mari.iso20022.agent.risk;

import com.mari.iso20022.agent.evidence.AssessedEvidenceItem;
import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.evidence.EvidenceCompleteness;
import com.mari.iso20022.agent.evidence.EvidenceQuality;
import com.mari.iso20022.investigation.dto.RiskLevel;
import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.validation.ValidationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class DeterministicRiskAgent implements RiskAgent {
    private static final Logger log = LoggerFactory.getLogger(DeterministicRiskAgent.class);
    private final RiskAgentProperties properties;

    public DeterministicRiskAgent(RiskAgentProperties properties) {
        this.properties = properties;
        if (properties.getScoreCap() <= 0 || properties.getCriticalThreshold() < properties.getHighThreshold()
                || properties.getHighThreshold() < properties.getMediumThreshold()
                || properties.getMediumThreshold() < 0 || properties.getCriticalPoints() < 0
                || properties.getHighPoints() < 0 || properties.getMediumPoints() < 0 || properties.getLowPoints() < 0
                || properties.getConfidenceThreshold().signum() < 0
                || properties.getConfidenceThreshold().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("risk-agent score cap and confidence threshold are invalid");
        }
    }

    @Override
    public RiskAgentResponse assess(RiskAgentRequest request) {
        long started = System.nanoTime();
        EvidenceAgentResponse evidence = request.evidenceAssessment();
        List<RiskFactor> factors = request.validationFindings().stream().filter(f -> !f.passed())
                .map(f -> factor(f, request)).toList();
        int score = Math.min(properties.getScoreCap(), factors.stream().mapToInt(RiskFactor::points).sum());
        RiskLevel level = riskLevel(score);
        BigDecimal confidence = confidence(evidence);
        boolean humanReview = level == RiskLevel.HIGH || level == RiskLevel.CRITICAL
                || evidence.evidenceQuality() == EvidenceQuality.INSUFFICIENT
                || confidence.compareTo(properties.getConfidenceThreshold()) < 0
                || evidence.conflictingEvidence();
        String action = action(level, humanReview, !factors.isEmpty());
        String explanation = explanation(score, factors, confidence, evidence);
        RiskAssessment assessment = new RiskAssessment(request.transactionId(), score, level, confidence,
                factors, action, humanReview, explanation);
        long duration = (System.nanoTime() - started) / 1_000_000;
        log.info("agent={} transactionId={} durationMs={} riskLevel={} riskScore={} factors={}",
                "RiskAgent", request.transactionId(), duration, level, score, factors.size());
        return new RiskAgentResponse(request.transactionId(), assessment, duration);
    }

    private RiskFactor factor(ValidationFinding finding, RiskAgentRequest request) {
        EvidenceAgentResponse evidence = request.evidenceAssessment();
        int points = switch (finding.severity()) {
            case CRITICAL -> properties.getCriticalPoints();
            case HIGH -> properties.getHighPoints();
            case MEDIUM -> properties.getMediumPoints();
            case LOW -> properties.getLowPoints();
        };
        List<String> references = evidence.evidenceItems().stream()
                .filter(item -> finding.ruleCode().equals(item.ruleCode()))
                .map(AssessedEvidenceItem::sourceReference).filter(ref -> ref != null && !ref.isBlank())
                .distinct().toList();
        String description = finding.message();
        if (!finding.evidence().isBlank()) description += " Evidence: " + finding.evidence();
        if (finding.ruleCode().equals("AMOUNT_ANOMALY") && request.historicalAnalysis().summary().averageAmount() != null) {
            var history = request.historicalAnalysis();
            description += " Historical same-currency baseline: average " + history.summary().averageAmount()
                    + " " + request.currency() + ", range " + history.summary().minimumAmount() + "-"
                    + history.summary().maximumAmount() + ", current-to-average ratio " + history.amountRatio() + ".";
        } else if (finding.ruleCode().equals("REPEATED_PAYMENT_FAILURE")) {
            description += " Persisted recent outcomes: " + request.historicalAnalysis().recentFailureCount()
                    + " failures, " + request.historicalAnalysis().recentSuccessCount() + " successes; no failure cause is inferred.";
        } else if (finding.ruleCode().equals("BENEFICIARY_MISMATCH")) {
            description += " Comparison uses " + request.historicalAnalysis().historicalBeneficiaries().stream()
                    .mapToLong(b -> b.successfulTransactionCount()).sum()
                    + " successful historical payment(s) for the normalized beneficiary name.";
        }
        return new RiskFactor(finding.ruleCode(), finding.severity(), points, description, references);
    }

    private static BigDecimal confidence(EvidenceAgentResponse evidence) {
        BigDecimal value = evidence.evidenceStrengthScore();
        if (evidence.evidenceCompleteness() == EvidenceCompleteness.PARTIAL) value = value.subtract(new BigDecimal("0.10"));
        if (evidence.conflictingEvidence()) value = value.subtract(new BigDecimal("0.20"));
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    private RiskLevel riskLevel(int score) {
        if (score >= properties.getCriticalThreshold()) return RiskLevel.CRITICAL;
        if (score >= properties.getHighThreshold()) return RiskLevel.HIGH;
        if (score >= properties.getMediumThreshold()) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private static String action(RiskLevel level, boolean humanReview, boolean hasRiskFactors) {
        return switch (level) {
            case CRITICAL -> "HOLD_AND_ESCALATE";
            case HIGH -> "MANUAL_REVIEW";
            case MEDIUM -> "REVIEW";
            case LOW -> humanReview ? "REVIEW" : hasRiskFactors ? "MONITOR" : "NO_ACTION";
        };
    }

    private static String explanation(int score, List<RiskFactor> factors, BigDecimal confidence,
                                      EvidenceAgentResponse evidence) {
        String factorCodes = factors.isEmpty() ? "no validation findings" : String.join(", ",
                factors.stream().map(RiskFactor::code).toList());
        String note = evidence.conflictingEvidence() ? " Conflicting beneficiary history requires human review." : "";
        if (evidence.evidenceQuality() == EvidenceQuality.INSUFFICIENT) note += " Evidence is insufficient; no unsupported finding was added.";
        return "Deterministic score " + score + " from " + factorCodes + ". Confidence " + confidence
                + " reflects evidence strength and completeness, not a probability." + note;
    }
}
