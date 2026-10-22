package com.mari.iso20022.investigation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HistoricalTransactionSummary(
        long transactionCount,
        long successfulTransactionCount,
        long failedTransactionCount,
        long amountSampleCount,
        BigDecimal averageAmount,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        OffsetDateTime mostRecentTransactionDate) {
}
