package com.mari.iso20022.validation.rules;

import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.validation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RepeatedPaymentFailureRule implements ValidationRule {
    private final int threshold;
    private final int windowDays;

    public RepeatedPaymentFailureRule(
            @Value("${investigation.rules.repeated-failure-threshold:3}") int threshold,
            @Value("${investigation.rules.recent-failure-window-days:90}") int windowDays) {
        this.threshold = threshold;
        this.windowDays = windowDays;
    }

    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        String debtorIban = context.payment().debtorIban();
        String beneficiary = BeneficiaryIdentity.normalize(context.payment().creditorName());
        Instant start = context.evaluatedAt().minus(Duration.ofDays(windowDays));
        List<PaymentTransaction> failures = context.historicalTransactions().stream()
                .filter(history -> history.getPaymentStatus() == PaymentStatus.FAILED)
                .filter(history -> history.getCreatedAt() != null && !history.getCreatedAt().isBefore(start))
                .filter(history -> sameAccount(history.getDebtorIban(), debtorIban))
                .filter(history -> BeneficiaryIdentity.normalize(history.getCreditorName()).equals(beneficiary))
                .toList();
        if (failures.size() < threshold) return Optional.empty();
        ValidationSeverity severity = failures.size() >= threshold * 2
                ? ValidationSeverity.HIGH : ValidationSeverity.MEDIUM;
        String evidence = failures.stream().map(PaymentTransaction::getTransactionId)
                .collect(Collectors.joining(", "));
        return Optional.of(ValidationFinding.failed("REPEATED_PAYMENT_FAILURE", severity,
                ValidationCategory.PAYMENT_HISTORY,
                "Repeated payment failures were recorded for this debtor and beneficiary",
                "debtorIban,creditorName", failures.size() + " failed transactions in the last " + windowDays
                        + " days (threshold " + threshold + "): " + evidence));
    }

    private boolean sameAccount(String left, String right) {
        return left != null && right != null
                && left.replaceAll("\\s", "").equalsIgnoreCase(right.replaceAll("\\s", ""));
    }
}
