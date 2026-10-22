package com.mari.iso20022.investigation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record HistoricalAnalysisResult(
        String transactionId,
        int analysisWindowDays,
        OffsetDateTime analysisWindowStart,
        OffsetDateTime analysisWindowEnd,
        HistoricalTransactionSummary summary,
        BigDecimal currentAmount,
        String currency,
        BigDecimal amountRatio,
        List<HistoricalBeneficiarySummary> historicalBeneficiaries,
        long recentFailureCount,
        long recentSuccessCount,
        long totalRecentTransactions,
        BigDecimal failureRate,
        OffsetDateTime mostRecentFailureDate,
        List<String> historicalTransactionIds) {
    public HistoricalAnalysisResult {
        historicalBeneficiaries = List.copyOf(historicalBeneficiaries);
        historicalTransactionIds = List.copyOf(historicalTransactionIds);
    }
}
