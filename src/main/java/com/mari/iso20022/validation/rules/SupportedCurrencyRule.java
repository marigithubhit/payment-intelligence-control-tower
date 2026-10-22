package com.mari.iso20022.validation.rules;

import com.mari.iso20022.validation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SupportedCurrencyRule implements ValidationRule {
    private final Set<String> supportedCurrencies;

    public SupportedCurrencyRule(@Value("${investigation.rules.supported-currencies:AED,USD,EUR,GBP}") String currencies) {
        supportedCurrencies = Arrays.stream(currencies.split(","))
                .map(String::trim).map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<ValidationFinding> evaluate(ValidationContext context) {
        String currency = context.payment().currency();
        if (currency != null && supportedCurrencies.contains(currency.toUpperCase(Locale.ROOT))) return Optional.empty();
        return Optional.of(ValidationFinding.failed("UNSUPPORTED_CURRENCY", ValidationSeverity.MEDIUM,
                ValidationCategory.CURRENCY, "Currency is not currently supported", "currency",
                "Observed currency: " + currency + "; configured supported currencies: " + supportedCurrencies));
    }
}
