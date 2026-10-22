package com.mari.iso20022.validation.rules;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.validation.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class RequiredFieldsRule implements ValidationRule {
    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        Iso20022Payment payment = context.payment();
        List<String> missing = new ArrayList<>();
        required(missing, "transactionId", payment.transactionId());
        required(missing, "messageId", payment.messageId());
        if (payment.amount() == null) missing.add("amount");
        required(missing, "currency", payment.currency());
        required(missing, "debtorName", payment.debtorName());
        required(missing, "creditorName", payment.creditorName());
        required(missing, "debtorIban", payment.debtorIban());
        required(missing, "creditorIban", payment.creditorIban());
        if (missing.isEmpty()) return Optional.empty();
        return Optional.of(ValidationFinding.failed("MISSING_REQUIRED_FIELD", ValidationSeverity.HIGH,
                ValidationCategory.REQUIRED_FIELD, "Required payment information is missing",
                String.join(",", missing), "Missing fields: " + String.join(", ", missing)));
    }

    private void required(List<String> missing, String field, String value) {
        if (value == null || value.isBlank()) missing.add(field);
    }
}
