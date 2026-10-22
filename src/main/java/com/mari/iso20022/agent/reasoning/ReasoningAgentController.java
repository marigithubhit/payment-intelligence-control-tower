package com.mari.iso20022.agent.reasoning;

import com.mari.iso20022.agent.evidence.*;
import com.mari.iso20022.agent.risk.RiskAssessment;
import com.mari.iso20022.investigation.dto.*;
import com.mari.iso20022.validation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agents/reasoning")
public class ReasoningAgentController {
    private final ReasoningAgent agent;

    public ReasoningAgentController(ReasoningAgent agent) { this.agent = agent; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ReasoningAgentResponse reason(@Valid @RequestBody Request body) {
        var payment = body.paymentSummary();
        var history = body.historicalAnalysis();
        var evidenceInput = body.evidenceAssessment();
        var riskInput = body.riskAssessment();
        EvidenceQuality quality = EvidenceQuality.valueOf(evidenceInput.quality());
        EvidenceCompleteness completeness = EvidenceCompleteness.valueOf(evidenceInput.completeness());
        var evidence = new EvidenceAgentResponse(body.transactionId(), List.of(), quality, completeness,
                evidenceInput.confidence(), List.of(), false, "Evidence assessment supplied by investigation", 0);
        var level = RiskLevel.valueOf(riskInput.riskLevel());
        String action = switch (level) {
            case LOW -> "MONITOR";
            case MEDIUM -> "REVIEW";
            case HIGH -> "MANUAL_REVIEW";
            case CRITICAL -> "HOLD_AND_ESCALATE";
        };
        var risk = new RiskAssessment(body.transactionId(), riskInput.riskScore(), level,
                riskInput.confidence(), List.of(), action, level == RiskLevel.HIGH || level == RiskLevel.CRITICAL,
                "Deterministic risk assessment supplied by investigation");
        var historical = new HistoricalAnalysisResult(body.transactionId(), 90, null, null,
                new HistoricalTransactionSummary(history.successfulTransactions(), history.successfulTransactions(), 0,
                        0, null, null, null, null), payment.amount(), payment.currency(), null,
                history.historicalBeneficiaryIban() == null ? List.of() : List.of(
                        new HistoricalBeneficiarySummary(history.historicalBeneficiaryIban(), history.successfulTransactions(), null)),
                0, history.successfulTransactions(), history.successfulTransactions(), BigDecimal.ZERO, null, List.of());
        var findings = body.validationFindings().stream().map(f -> new ValidationFinding(f.ruleCode(),
                ValidationSeverity.valueOf(f.severity()), categoryFor(f.ruleCode()), f.message(), null,
                f.message(), false)).toList();
        var items = body.evidenceItems().stream().map(i -> new AssessedEvidenceItem(classificationFor(i.type()),
                i.type(), i.type(), null, i.description(), null, null)).toList();
        var reasoningPayment = new ReasoningPaymentSummary(null, body.transactionId(), 1, null, null, null,
                payment.amount(), payment.currency(), null, payment.debtorName(), payment.debtorIban(), null,
                payment.creditorName(), payment.creditorIban(), null, null);
        return agent.reason(new ReasoningAgentRequest(body.transactionId(), reasoningPayment, findings, historical,
                evidence, risk, items));
    }

    private static ValidationCategory categoryFor(String code) {
        if (code != null && code.contains("BENEFICIARY")) return ValidationCategory.BENEFICIARY;
        if (code != null && code.contains("AMOUNT")) return ValidationCategory.AMOUNT;
        if (code != null && code.contains("IBAN")) return ValidationCategory.ACCOUNT;
        return ValidationCategory.PAYMENT_HISTORY;
    }
    private static EvidenceClassification classificationFor(String type) {
        if (type != null && type.contains("BENEFICIARY")) return EvidenceClassification.BENEFICIARY;
        return EvidenceClassification.VALIDATION;
    }

    public record Request(@NotBlank String transactionId, @NotNull @Valid PaymentSummary paymentSummary,
                          @NotNull List<@Valid Finding> validationFindings,
                          @NotNull @Valid HistoricalAnalysis historicalAnalysis,
                          @NotNull @Valid EvidenceAssessment evidenceAssessment,
                          @NotNull @Valid RiskInput riskAssessment,
                          @NotNull List<@Valid EvidenceItem> evidenceItems) { }
    public record PaymentSummary(@NotNull @DecimalMin("0.01") BigDecimal amount,
                                 @NotBlank String currency, @NotBlank String debtorName,
                                 @NotBlank String debtorIban, @NotBlank String creditorName,
                                 @NotBlank String creditorIban) { }
    public record Finding(@NotBlank String ruleCode, @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL") String severity, @NotBlank String message) { }
    public record HistoricalAnalysis(@Min(0) long successfulTransactions, String historicalBeneficiaryIban) { }
    public record EvidenceAssessment(@Pattern(regexp = "STRONG|MODERATE|WEAK|INSUFFICIENT") String quality,
                                     @Pattern(regexp = "COMPLETE|PARTIAL|INSUFFICIENT") String completeness,
                                     @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal confidence) { }
    public record RiskInput(@Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL") String riskLevel, @Min(0) int riskScore,
                            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal confidence) { }
    public record EvidenceItem(@NotBlank String type, @NotBlank String description) { }
}
