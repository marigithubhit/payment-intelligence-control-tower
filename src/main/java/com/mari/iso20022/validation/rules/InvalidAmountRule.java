package com.mari.iso20022.validation.rules;

import com.mari.iso20022.validation.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InvalidAmountRule implements ValidationRule {
    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        if (context.payment().amount() != null && context.payment().amount().signum() > 0) return Optional.empty();
        return Optional.of(ValidationFinding.failed("INVALID_AMOUNT", ValidationSeverity.HIGH,
                ValidationCategory.AMOUNT, "Payment amount must be greater than zero", "amount",
                "Observed amount: " + context.payment().amount()));
    }
}
