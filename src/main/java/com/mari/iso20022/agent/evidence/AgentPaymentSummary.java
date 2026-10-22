package com.mari.iso20022.agent.evidence;

import java.math.BigDecimal;

public record AgentPaymentSummary(String transactionId, BigDecimal amount, String currency,
                                  String creditorIban) {
}
