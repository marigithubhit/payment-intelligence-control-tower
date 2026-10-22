package com.mari.iso20022.investigation.dto;

import java.time.OffsetDateTime;

public record HistoricalBeneficiarySummary(String creditorIban, long successfulTransactionCount,
                                           OffsetDateTime mostRecentTransactionDate) {
}
