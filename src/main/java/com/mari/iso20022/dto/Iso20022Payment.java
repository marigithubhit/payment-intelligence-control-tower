package com.mari.iso20022.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Iso20022Payment(
        String messageId,
        OffsetDateTime creationDateTime,
        int numberOfTransactions,
        String settlementMethod,
        String instructionId,
        String endToEndId,
        String transactionId,
        BigDecimal amount,
        String currency,
        LocalDate settlementDate,
        String debtorName,
        String debtorIban,
        String debtorBic,
        String creditorName,
        String creditorIban,
        String creditorBic,
        String remittanceInformation) {
}
