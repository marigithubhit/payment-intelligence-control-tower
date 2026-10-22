package com.mari.iso20022.dto;

import java.math.BigDecimal;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.validation.ValidationFinding;

import java.util.List;

public record PaymentIngestionResponse(
        String status,
        String messageId,
        String transactionId,
        BigDecimal amount,
        String currency,
        String debtor,
        String creditor,
        String debtorIban,
        String creditorIban,
        boolean persisted,
        List<ValidationFinding> findings) {

    public static PaymentIngestionResponse received(PaymentTransaction payment, List<ValidationFinding> findings) {
        return new PaymentIngestionResponse(payment.getPaymentStatus().name(), payment.getMessageId(),
                payment.getTransactionId(), payment.getAmount(), payment.getCurrency(), payment.getDebtorName(),
                payment.getCreditorName(), payment.getDebtorIban(), payment.getCreditorIban(), true, List.copyOf(findings));
    }
}
