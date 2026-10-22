package com.mari.iso20022.agent.evidence;

import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.HistoricalBeneficiarySummary;
import com.mari.iso20022.investigation.dto.HistoricalTransactionSummary;
import com.mari.iso20022.investigation.dto.InvestigationEvidenceResponse;
import com.mari.iso20022.validation.ValidationCategory;
import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.validation.ValidationSeverity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicEvidenceAgentTest {
    private final EvidenceAgent agent = new DeterministicEvidenceAgent();

    @Test
    void reportsInsufficientWhenNoEvidenceOrHistoryExists() {
        EvidenceAgentResponse result = agent.analyze(request(List.of(), List.of(), history(0, List.of(), 0)));
        assertEquals(EvidenceQuality.INSUFFICIENT, result.evidenceQuality());
        assertEquals(EvidenceCompleteness.INSUFFICIENT, result.evidenceCompleteness());
        assertEquals(new BigDecimal("0.00"), result.evidenceStrengthScore());
        assertTrue(result.evidenceItems().isEmpty());
        assertTrue(result.missingEvidence().stream().anyMatch(gap -> gap.contains("No historical transactions")));
    }

    @Test
    void preservesEvidenceProvenanceAndClassifiesSingleFinding() {
        var persisted = evidence("VALIDATION_FINDING", "ACCOUNT", "INVALID_ACCOUNT_FORMAT", "Phase4ValidationEngine");
        EvidenceAgentResponse result = agent.analyze(request(
                List.of(finding("INVALID_ACCOUNT_FORMAT", ValidationSeverity.HIGH)), List.of(persisted), history(0, List.of(), 0)));
        assertEquals(EvidenceClassification.ACCOUNT, result.evidenceItems().getFirst().classification());
        assertEquals("VALIDATION_FINDING", result.evidenceItems().getFirst().evidenceType());
        assertEquals("INVALID_ACCOUNT_FORMAT", result.evidenceItems().getFirst().ruleCode());
        assertEquals("Phase4ValidationEngine", result.evidenceItems().getFirst().sourceReference());
        assertEquals(EvidenceQuality.WEAK, result.evidenceQuality());
        assertEquals(EvidenceCompleteness.PARTIAL, result.evidenceCompleteness());
    }

    @Test
    void includesPhaseFourFindingWhenPersistedCaseEvidenceIsMissingIt() {
        var finding = finding("INVALID_ACCOUNT_FORMAT", ValidationSeverity.HIGH);
        EvidenceAgentResponse result = agent.analyze(request(List.of(finding), List.of(), history(0, List.of(), 0)));
        assertEquals(1, result.evidenceItems().size());
        assertEquals("VALIDATION_FINDING", result.evidenceItems().getFirst().evidenceType());
        assertEquals("INVALID_ACCOUNT_FORMAT", result.evidenceItems().getFirst().ruleCode());
        assertEquals("Phase4ValidationEngine", result.evidenceItems().getFirst().sourceReference());
    }

    @Test
    void multipleIndependentHistoricalFactsProvideStrongQualityWithoutInventingItems() {
        var finding = finding("BENEFICIARY_MISMATCH", ValidationSeverity.HIGH);
        var validation = evidence("VALIDATION_FINDING", "BENEFICIARY", finding.ruleCode(), "Phase4ValidationEngine");
        var historical = evidence("HISTORICAL_BENEFICIARY", "BENEFICIARY", finding.ruleCode(), "PaymentTransactionRepository");
        EvidenceAgentResponse result = agent.analyze(request(List.of(finding), List.of(validation, historical), history(3,
                List.of(new HistoricalBeneficiarySummary("AE120331987654321098765", 3,
                        OffsetDateTime.parse("2026-10-01T00:00:00Z"))), 3)));
        assertEquals(2, result.evidenceItems().size());
        assertEquals(EvidenceQuality.STRONG, result.evidenceQuality());
        assertTrue(result.summary().contains("not a probability"));
        assertTrue(result.evidenceItems().stream().allMatch(item -> item.sourceReference() != null));
    }

    @Test
    void flagsCurrentAccountThatAlsoAppearsAlongsideAnotherHistoricalAccount() {
        HistoricalAnalysisResult analysis = history(3, List.of(
                new HistoricalBeneficiarySummary("AE120331987654321098765", 2, null),
                new HistoricalBeneficiarySummary("AE990331111111111111111", 1, null)), 3);
        var result = agent.analyze(new EvidenceAgentRequest(
                new AgentPaymentSummary("TXN1", new BigDecimal("500"), "AED", "AE120331987654321098765"),
                List.of(), analysis, List.of(evidence("HISTORICAL_BENEFICIARY", "BENEFICIARY", null,
                        "PaymentTransactionRepository"))));
        assertTrue(result.conflictingEvidence());
        assertTrue(result.summary().contains("1 persisted evidence item"));
    }

    static ValidationFinding finding(String code, ValidationSeverity severity) {
        return ValidationFinding.failed(code, severity, ValidationCategory.BENEFICIARY,
                "Observed validation finding", "creditorIban", "Evidence from persisted records");
    }

    static InvestigationEvidenceResponse evidence(String type, String category, String ruleCode, String source) {
        return new InvestigationEvidenceResponse(type, category, ruleCode, null, "Persisted fact", source,
                "observed", "expected", null);
    }

    static EvidenceAgentRequest request(List<ValidationFinding> findings, List<InvestigationEvidenceResponse> evidence,
                                        HistoricalAnalysisResult history) {
        return new EvidenceAgentRequest(new AgentPaymentSummary("TXN1", new BigDecimal("500"), "AED",
                "AE120331987654321098765"), findings, history, evidence);
    }

    static HistoricalAnalysisResult history(long transactions, List<HistoricalBeneficiarySummary> beneficiaries,
                                            long successes) {
        var summary = new HistoricalTransactionSummary(transactions, successes, transactions - successes,
                successes, successes == 0 ? null : new BigDecimal("100.00"), null, null, null);
        return new HistoricalAnalysisResult("TXN1", 90, null, null, summary, new BigDecimal("500.00"),
                "AED", null, beneficiaries, transactions - successes, successes, transactions,
                transactions == 0 ? null : new BigDecimal("0.00"), null, List.of());
    }
}
