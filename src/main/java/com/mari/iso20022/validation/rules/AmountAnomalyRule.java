package com.mari.iso20022.validation.rules;

import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.validation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AmountAnomalyRule implements ValidationRule {
    private static final int MINIMUM_HISTORY = 3;
    private final BigDecimal multiplier;

    public AmountAnomalyRule(@Value("${investigation.rules.amount-anomaly-multiplier:3.0}") BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        BigDecimal currentAmount = context.payment().amount();
        if (currentAmount == null || currentAmount.signum() <= 0) return Optional.empty();
        String debtor = normalizeAccount(context.payment().debtorIban());
        String beneficiary = BeneficiaryIdentity.normalize(context.payment().creditorName());
        List<PaymentTransaction> history = context.historicalTransactions().stream()
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                .filter(payment -> normalizeAccount(payment.getDebtorIban()).equals(debtor))
                .filter(payment -> BeneficiaryIdentity.normalize(payment.getCreditorName()).equals(beneficiary))
                .filter(payment -> context.payment().currency() != null
                        && context.payment().currency().equalsIgnoreCase(payment.getCurrency()))
                .filter(payment -> payment.getAmount() != null && payment.getAmount().signum() > 0)
                .toList();
        if (history.size() < MINIMUM_HISTORY) return Optional.empty();
        BigDecimal total = history.stream().map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(history.size()), MathContext.DECIMAL128);
        BigDecimal boundary = average.multiply(multiplier);
        if (currentAmount.compareTo(boundary) <= 0) return Optional.empty();
        ValidationSeverity severity = currentAmount.compareTo(boundary.multiply(BigDecimal.valueOf(2))) > 0
                ? ValidationSeverity.HIGH : ValidationSeverity.MEDIUM;
        String ids = history.stream().map(PaymentTransaction::getTransactionId).collect(Collectors.joining(", "));
        return Optional.of(ValidationFinding.failed("AMOUNT_ANOMALY", severity, ValidationCategory.AMOUNT,
                "Payment amount is significantly above the historical average for this relationship", "amount",
                "Current amount " + currentAmount + " exceeds " + multiplier + "x historical average "
                        + average.setScale(2, java.math.RoundingMode.HALF_UP) + " from " + history.size()
                        + " successful payments (" + ids + "). This is an anomaly indicator, not a fraud conclusion."));
    }

    private String normalizeAccount(String value) {
        return value == null ? "" : value.replaceAll("\\s", "").toUpperCase(java.util.Locale.ROOT);
    }
}
