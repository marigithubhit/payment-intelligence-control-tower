package com.mari.iso20022.validation;

import java.util.Optional;

public interface ValidationRule {
    Optional<ValidationFinding> evaluate(ValidationContext context);
}
