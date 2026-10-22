package com.mari.iso20022.validation;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.investigation.dto.HistoricalPaymentEvidence;
import com.mari.iso20022.investigation.HistoricalAnalysisProperties;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentValidationService {
    private final List<ValidationRule> rules;
    private final PaymentTransactionRepository repository;
    private final int windowDays;

    public PaymentValidationService(List<ValidationRule> rules, PaymentTransactionRepository repository,
                                    HistoricalAnalysisProperties properties) {
        this.rules = List.copyOf(rules);
        this.repository = repository;
        this.windowDays = properties.getWindowDays();
    }

    public PaymentValidationResult validate(Iso20022Payment payment) {
        return validate(payment, null);
    }

    public PaymentValidationResult validate(PaymentTransaction current) {
        return validate(current.toPayment(), current.getId());
    }

    private PaymentValidationResult validate(Iso20022Payment payment, UUID currentId) {
        List<PaymentTransaction> history = historicalPayments(payment, currentId);
        boolean duplicate = payment.transactionId() != null && (currentId == null
                ? repository.existsByTransactionId(payment.transactionId())
                : repository.existsByTransactionIdAndIdNot(payment.transactionId(), currentId));
        ValidationContext context = new ValidationContext(payment, history, duplicate, Instant.now());
        List<ValidationFinding> findings = rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(ValidationFinding::ruleCode))
                .toList();

        String beneficiary = BeneficiaryIdentity.normalize(payment.creditorName());
        List<HistoricalPaymentEvidence> evidence = history.stream()
                .filter(transaction -> BeneficiaryIdentity.normalize(transaction.getCreditorName()).equals(beneficiary))
                .map(PaymentValidationService::toEvidence)
                .toList();
        return new PaymentValidationResult(findings, evidence);
    }

    private List<PaymentTransaction> historicalPayments(Iso20022Payment payment, UUID currentId) {
        String debtorIban = payment.debtorIban();
        if (debtorIban == null || debtorIban.isBlank() || payment.creationDateTime() == null) return List.of();
        OffsetDateTime end = payment.creationDateTime();
        OffsetDateTime start = end.minusDays(windowDays);
        return currentId == null
                ? repository.findByDebtorIbanIgnoreCaseAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualOrderByCreationDateTimeDesc(
                    debtorIban, start, end)
                : repository.findByDebtorIbanIgnoreCaseAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualAndIdNotOrderByCreationDateTimeDesc(
                    debtorIban, start, end, currentId);
    }

    private static HistoricalPaymentEvidence toEvidence(PaymentTransaction transaction) {
        return new HistoricalPaymentEvidence(transaction.getTransactionId(), transaction.getPaymentStatus(),
                transaction.getAmount(), transaction.getCurrency(), transaction.getCreditorName(),
                transaction.getCreditorIban(), transaction.getCreatedAt());
    }
}
