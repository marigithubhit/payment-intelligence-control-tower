package com.mari.iso20022.investigation.dto;

import com.mari.iso20022.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalPaymentEvidence(String transactionId,
                                        PaymentStatus paymentStatus,
                                        BigDecimal amount,
                                        String currency,
                                        String creditorName,
                                        String creditorIban,
                                        Instant createdAt) {
}
