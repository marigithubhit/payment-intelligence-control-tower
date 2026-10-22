package com.mari.iso20022.validation;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.entity.PaymentTransaction;

import java.time.Instant;
import java.util.List;

public record ValidationContext(Iso20022Payment payment,
                                List<PaymentTransaction> historicalTransactions,
                                boolean duplicateTransaction,
                                Instant evaluatedAt) {
}
