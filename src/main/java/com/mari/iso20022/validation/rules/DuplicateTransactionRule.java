package com.mari.iso20022.validation.rules;

import com.mari.iso20022.validation.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DuplicateTransactionRule implements ValidationRule {
    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        if (!context.duplicateTransaction()) return Optional.empty();
        return Optional.of(ValidationFinding.failed("DUPLICATE_TRANSACTION", ValidationSeverity.HIGH,
                ValidationCategory.DUPLICATE, "Transaction ID has already been received", "transactionId",
                "A persisted payment already uses transaction ID " + context.payment().transactionId()));
    }
}
