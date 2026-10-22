package com.mari.iso20022.validation.rules;

import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.validation.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BeneficiaryMismatchRule implements ValidationRule {
    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        String debtorIban = context.payment().debtorIban();
        String beneficiary = BeneficiaryIdentity.normalize(context.payment().creditorName());
        String currentAccount = normalizeAccount(context.payment().creditorIban());
        List<PaymentTransaction> successfulHistory = context.historicalTransactions().stream()
                .filter(history -> history.getPaymentStatus() == PaymentStatus.SUCCESS)
                .filter(history -> BeneficiaryIdentity.normalize(history.getCreditorName()).equals(beneficiary))
                .filter(history -> sameAccount(history.getDebtorIban(), debtorIban))
                .toList();
        if (successfulHistory.isEmpty() || successfulHistory.stream()
                .anyMatch(history -> sameAccount(history.getCreditorIban(), currentAccount))) return Optional.empty();
        String evidence = successfulHistory.stream().map(this::reference)
                .collect(Collectors.joining("; ", "Historical successful payment evidence: ", ""));
        return Optional.of(ValidationFinding.failed("BENEFICIARY_MISMATCH", ValidationSeverity.HIGH,
                ValidationCategory.BENEFICIARY, "Creditor account differs from historical successful payments",
                "creditorIban", "Current creditor IBAN differs from " + evidence));
    }

    private String reference(PaymentTransaction payment) {
        return payment.getTransactionId() + " used " + payment.getCreditorIban();
    }

    private String normalizeAccount(String value) {
        return value == null ? "" : value.replaceAll("\\s", "").toUpperCase(java.util.Locale.ROOT);
    }

    private boolean sameAccount(String left, String right) {
        return normalizeAccount(left).equals(normalizeAccount(right));
    }
}
