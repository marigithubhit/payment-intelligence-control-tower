package com.mari.iso20022.validation;

import com.mari.iso20022.investigation.dto.HistoricalPaymentEvidence;

import java.util.List;

public record PaymentValidationResult(List<ValidationFinding> findings,
                                      List<HistoricalPaymentEvidence> historicalEvidence) {
    public PaymentValidationResult {
        findings = List.copyOf(findings);
        historicalEvidence = List.copyOf(historicalEvidence);
    }
}
