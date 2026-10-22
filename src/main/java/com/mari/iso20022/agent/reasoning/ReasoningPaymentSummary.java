package com.mari.iso20022.agent.reasoning;

import com.mari.iso20022.dto.Iso20022Payment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReasoningPaymentSummary(String messageId, String transactionId,
                                     int numberOfTransactions, String settlementMethod,
                                     String instructionId, String endToEndId,
                                     BigDecimal amount, String currency, LocalDate settlementDate,
                                     String debtorName, String debtorIban, String debtorBic,
                                     String creditorName, String creditorIban, String creditorBic,
                                     String remittanceInformation) {
    public static ReasoningPaymentSummary from(Iso20022Payment payment) {
        return new ReasoningPaymentSummary(payment.messageId(), payment.transactionId(),
                payment.numberOfTransactions(), payment.settlementMethod(), payment.instructionId(),
                payment.endToEndId(), payment.amount(), payment.currency(), payment.settlementDate(),
                payment.debtorName(), payment.debtorIban(), payment.debtorBic(), payment.creditorName(),
                payment.creditorIban(), payment.creditorBic(), payment.remittanceInformation());
    }
}
