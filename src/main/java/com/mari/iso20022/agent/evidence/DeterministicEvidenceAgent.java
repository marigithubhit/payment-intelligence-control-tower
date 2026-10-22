package com.mari.iso20022.agent.evidence;

import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.InvestigationEvidenceResponse;
import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.validation.ValidationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DeterministicEvidenceAgent implements EvidenceAgent {
    private static final Logger log = LoggerFactory.getLogger(DeterministicEvidenceAgent.class);

    @Override
    public EvidenceAgentResponse analyze(EvidenceAgentRequest request) {
        long started = System.nanoTime();
        List<AssessedEvidenceItem> items = collect(request);
        List<String> missing = identifyGaps(request);
        boolean conflicting = conflictingBeneficiaryEvidence(request);
        BigDecimal score = strengthScore(request, items);
        EvidenceQuality quality = quality(score);
        EvidenceCompleteness completeness = items.isEmpty() ? EvidenceCompleteness.INSUFFICIENT
                : missing.isEmpty() ? EvidenceCompleteness.COMPLETE : EvidenceCompleteness.PARTIAL;
        long supportedFindings = request.validationFindings().stream()
                .filter(f -> items.stream().anyMatch(i -> f.ruleCode().equals(i.ruleCode())))
                .count();
        String summary = "Assessed " + items.size() + " persisted evidence item(s); " + supportedFindings
                + " validation finding(s) have a matching evidence rule reference. Evidence strength is an internal orchestration score, not a probability.";
        long duration = (System.nanoTime() - started) / 1_000_000;
        log.info("agent={} transactionId={} durationMs={} evidenceItems={} quality={}",
                "EvidenceAgent", request.payment().transactionId(), duration, items.size(), quality);
        return new EvidenceAgentResponse(request.payment().transactionId(), items, quality, completeness,
                score, missing, conflicting, summary, duration);
    }

    private static AssessedEvidenceItem assessItem(InvestigationEvidenceResponse evidence) {
        return new AssessedEvidenceItem(classify(evidence), evidence.evidenceType(), evidence.ruleCode(),
                evidence.sourceReference(), evidence.description(), evidence.observedValue(), evidence.expectedValue());
    }

    private static List<AssessedEvidenceItem> collect(EvidenceAgentRequest request) {
        List<AssessedEvidenceItem> items = new ArrayList<>(request.investigationEvidence().stream()
                .map(DeterministicEvidenceAgent::assessItem).toList());
        Set<String> representedRules = items.stream().map(AssessedEvidenceItem::ruleCode)
                .filter(rule -> rule != null).collect(Collectors.toSet());
        request.validationFindings().stream().filter(f -> !representedRules.contains(f.ruleCode()))
                .map(f -> new AssessedEvidenceItem(classify(f.category()), "VALIDATION_FINDING", f.ruleCode(),
                        "Phase4ValidationEngine", f.message() + " Evidence: " + f.evidence(), null, null))
                .forEach(items::add);
        return List.copyOf(items);
    }

    private static EvidenceClassification classify(ValidationCategory category) {
        return switch (category) {
            case AMOUNT -> EvidenceClassification.AMOUNT;
            case ACCOUNT -> EvidenceClassification.ACCOUNT;
            case BENEFICIARY -> EvidenceClassification.BENEFICIARY;
            case PAYMENT_HISTORY -> EvidenceClassification.FAILURE_PATTERN;
            case CURRENCY -> EvidenceClassification.PAYMENT;
            case REQUIRED_FIELD, DUPLICATE -> EvidenceClassification.VALIDATION;
        };
    }

    private static EvidenceClassification classify(InvestigationEvidenceResponse evidence) {
        String type = evidence.evidenceType() == null ? "" : evidence.evidenceType().toUpperCase(Locale.ROOT);
        if (type.contains("BENEFICIARY")) return EvidenceClassification.BENEFICIARY;
        if (type.contains("AMOUNT")) return EvidenceClassification.AMOUNT;
        if (type.contains("FAILURE")) return EvidenceClassification.FAILURE_PATTERN;
        if (type.contains("HISTORICAL")) return EvidenceClassification.HISTORICAL;
        if (type.equals("VALIDATION_FINDING")) {
            String category = evidence.category() == null ? "" : evidence.category().toUpperCase(Locale.ROOT);
            if (category.contains("ACCOUNT")) return EvidenceClassification.ACCOUNT;
            if (category.contains("AMOUNT")) return EvidenceClassification.AMOUNT;
            if (category.contains("BENEFICIARY")) return EvidenceClassification.BENEFICIARY;
            if (category.contains("PAYMENT_HISTORY")) return EvidenceClassification.FAILURE_PATTERN;
            if (category.contains("PAYMENT")) return EvidenceClassification.PAYMENT;
            return EvidenceClassification.VALIDATION;
        }
        return EvidenceClassification.PAYMENT;
    }

    private static List<String> identifyGaps(EvidenceAgentRequest request) {
        List<String> missing = new ArrayList<>();
        HistoricalAnalysisResult history = request.historicalAnalysis();
        if (history.summary().transactionCount() == 0) {
            missing.add("No historical transactions are available in the configured analysis window.");
        } else if (history.summary().successfulTransactionCount() == 0) {
            missing.add("No historical successful transactions are available for beneficiary or amount comparison.");
        }
        if (history.recentFailureCount() > 0) {
            missing.add("Failure reason is unavailable in the persisted payment history.");
        }
        if (!hasProvenance(request.investigationEvidence()) && !request.investigationEvidence().isEmpty()) {
            missing.add("Persisted evidence source provenance is incomplete.");
        }
        return List.copyOf(missing);
    }

    private static boolean hasProvenance(List<InvestigationEvidenceResponse> evidence) {
        return evidence.stream().allMatch(e -> e.evidenceType() != null
                && e.sourceReference() != null && !e.sourceReference().isBlank());
    }

    private static boolean conflictingBeneficiaryEvidence(EvidenceAgentRequest request) {
        Set<String> historicalAccounts = request.historicalAnalysis().historicalBeneficiaries().stream()
                .map(b -> normalize(b.creditorIban())).collect(Collectors.toSet());
        String currentAccount = normalize(request.payment().creditorIban());
        return currentAccount != null && historicalAccounts.contains(currentAccount) && historicalAccounts.size() > 1;
    }

    private static String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    private static BigDecimal strengthScore(EvidenceAgentRequest request, List<AssessedEvidenceItem> items) {
        if (items.isEmpty()) return new BigDecimal("0.00");
        BigDecimal score = new BigDecimal("0.40");
        if (request.historicalAnalysis().summary().transactionCount() > 0) score = score.add(new BigDecimal("0.05"));
        if (items.stream().anyMatch(i -> i.evidenceType().startsWith("HISTORICAL_"))) score = score.add(new BigDecimal("0.20"));
        if (request.validationFindings().stream().anyMatch(f -> items.stream()
                .anyMatch(i -> f.ruleCode().equals(i.ruleCode()) && i.evidenceType().startsWith("HISTORICAL_")))) {
            score = score.add(new BigDecimal("0.15"));
        }
        if (items.stream().map(AssessedEvidenceItem::classification).distinct().count() >= 2) {
            score = score.add(new BigDecimal("0.10"));
        }
        if (items.stream().allMatch(i -> i.sourceReference() != null && !i.sourceReference().isBlank())) {
            score = score.add(new BigDecimal("0.10"));
        }
        if (conflictingBeneficiaryEvidence(request)) score = score.subtract(new BigDecimal("0.10"));
        return score.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    private static EvidenceQuality quality(BigDecimal score) {
        if (score.compareTo(new BigDecimal("0.90")) >= 0) return EvidenceQuality.STRONG;
        if (score.compareTo(new BigDecimal("0.70")) >= 0) return EvidenceQuality.MODERATE;
        if (score.compareTo(new BigDecimal("0.40")) >= 0) return EvidenceQuality.WEAK;
        return EvidenceQuality.INSUFFICIENT;
    }
}
