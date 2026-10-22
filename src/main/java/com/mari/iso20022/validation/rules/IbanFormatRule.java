package com.mari.iso20022.validation.rules;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.validation.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class IbanFormatRule implements ValidationRule {
    private static final Pattern BASIC_IBAN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$");

    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        Iso20022Payment payment = context.payment();
        List<String> invalid = new ArrayList<>();
        check(invalid, "debtorIban", payment.debtorIban());
        check(invalid, "creditorIban", payment.creditorIban());
        if (invalid.isEmpty()) return Optional.empty();
        return Optional.of(ValidationFinding.failed("INVALID_ACCOUNT_FORMAT", ValidationSeverity.HIGH,
                ValidationCategory.ACCOUNT, "IBAN format is invalid", String.join(",", invalid),
                "Basic IBAN format check failed for: " + String.join(", ", invalid)
                        + ". This format check does not verify that an account exists."));
    }

    private void check(List<String> invalid, String field, String iban) {
        if (iban == null || iban.isBlank()) return;
        String normalized = iban.replaceAll("\\s", "").toUpperCase(java.util.Locale.ROOT);
        if (!BASIC_IBAN.matcher(normalized).matches()) invalid.add(field);
    }
}
