package com.mari.iso20022.service;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.dto.PaymentIngestionResponse;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.exception.DuplicateTransactionException;
import com.mari.iso20022.parser.Pacs008Parser;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import com.mari.iso20022.validation.PaymentValidationResult;
import com.mari.iso20022.validation.PaymentValidationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentIngestionService {
    private final Pacs008Parser parser;
    private final PaymentTransactionRepository repository;
    private final PaymentValidationService validationService;

    public PaymentIngestionService(Pacs008Parser parser, PaymentTransactionRepository repository,
                                   PaymentValidationService validationService) {
        this.parser = parser;
        this.repository = repository;
        this.validationService = validationService;
    }

    @Transactional
    public PaymentIngestionResponse ingest(String xml) {
        Iso20022Payment payment = parser.parse(xml);
        PaymentValidationResult validation = validationService.validate(payment);
        if (validation.findings().stream().anyMatch(finding -> finding.ruleCode().equals("DUPLICATE_TRANSACTION"))) {
            throw new DuplicateTransactionException(payment.transactionId());
        }
        try {
            PaymentTransaction saved = repository.saveAndFlush(PaymentTransaction.received(payment));
            return PaymentIngestionResponse.received(saved, validation.findings());
        } catch (DataIntegrityViolationException exception) {
            if (isTransactionIdConflict(exception)) {
                throw new DuplicateTransactionException(payment.transactionId(), exception);
            }
            throw exception;
        }
    }

    private boolean isTransactionIdConflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uk_payment_transaction_transaction_id")
                    || message.toLowerCase().contains("transaction_id"))) return true;
        }
        return false;
    }
}
