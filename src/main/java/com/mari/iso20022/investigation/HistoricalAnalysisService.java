package com.mari.iso20022.investigation;

import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.exception.TransactionNotFoundException;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.HistoricalBeneficiarySummary;
import com.mari.iso20022.investigation.dto.HistoricalTransactionSummary;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import com.mari.iso20022.validation.BeneficiaryIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HistoricalAnalysisService {
    private final PaymentTransactionRepository repository;
    private final int windowDays;

    public HistoricalAnalysisService(PaymentTransactionRepository repository, HistoricalAnalysisProperties properties) {
        int windowDays = properties.getWindowDays();
        if (windowDays <= 0) throw new IllegalArgumentException("historical-analysis.window-days must be positive");
        this.repository = repository;
        this.windowDays = windowDays;
    }

    @Transactional(readOnly = true)
    public HistoricalAnalysisResult analyzeByTransactionId(String transactionId) {
        PaymentTransaction current = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return analyze(current);
    }

    @Transactional(readOnly = true)
    public HistoricalAnalysisResult analyze(PaymentTransaction current) {
        OffsetDateTime end = current.getCreationDateTime();
        OffsetDateTime start = end.minusDays(windowDays);
        List<PaymentTransaction> history = current.getDebtorIban() == null || current.getDebtorIban().isBlank()
                ? List.of()
                : repository.findByDebtorIbanIgnoreCaseAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualAndIdNotOrderByCreationDateTimeDesc(
                    current.getDebtorIban(), start, end, current.getId());
        List<PaymentTransaction> successful = history.stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.SUCCESS).toList();
        String beneficiary = BeneficiaryIdentity.normalize(current.getCreditorName());
        List<PaymentTransaction> matchingBeneficiary = successful.stream()
                .filter(t -> BeneficiaryIdentity.normalize(t.getCreditorName()).equals(beneficiary)).toList();
        List<PaymentTransaction> amountSamples = matchingBeneficiary.stream()
                .filter(t -> t.getCurrency().equalsIgnoreCase(current.getCurrency())).toList();
        BigDecimal average = average(amountSamples);
        BigDecimal min = amountSamples.stream().map(PaymentTransaction::getAmount).min(BigDecimal::compareTo).orElse(null);
        BigDecimal max = amountSamples.stream().map(PaymentTransaction::getAmount).max(BigDecimal::compareTo).orElse(null);
        BigDecimal ratio = average == null || average.signum() == 0 ? null
                : current.getAmount().divide(average, 2, RoundingMode.HALF_UP);
        OffsetDateTime mostRecent = history.stream().map(PaymentTransaction::getCreationDateTime)
                .max(OffsetDateTime::compareTo).orElse(null);
        HistoricalTransactionSummary summary = new HistoricalTransactionSummary(history.size(), successful.size(),
                history.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.FAILED).count(),
                amountSamples.size(), average, min, max, mostRecent);

        Map<String, List<PaymentTransaction>> byAccount = matchingBeneficiary.stream()
                .collect(Collectors.groupingBy(PaymentTransaction::getCreditorIban));
        List<HistoricalBeneficiarySummary> beneficiaries = byAccount.entrySet().stream()
                .map(e -> new HistoricalBeneficiarySummary(e.getKey(), e.getValue().size(), e.getValue().stream()
                        .map(PaymentTransaction::getCreationDateTime).max(OffsetDateTime::compareTo).orElse(null)))
                .sorted((a, b) -> Long.compare(b.successfulTransactionCount(), a.successfulTransactionCount()))
                .toList();
        long failures = summary.failedTransactionCount();
        long successes = summary.successfulTransactionCount();
        long total = history.size();
        BigDecimal failureRate = total == 0 ? null : BigDecimal.valueOf(failures * 100L)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        OffsetDateTime mostRecentFailure = history.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.FAILED)
                .map(PaymentTransaction::getCreationDateTime).max(OffsetDateTime::compareTo).orElse(null);
        List<String> ids = history.stream().map(PaymentTransaction::getTransactionId).toList();
        return new HistoricalAnalysisResult(current.getTransactionId(), windowDays, start, end, summary,
                current.getAmount(), current.getCurrency(), ratio, beneficiaries, failures, successes,
                total, failureRate, mostRecentFailure, ids);
    }

    private static BigDecimal average(List<PaymentTransaction> values) {
        if (values.isEmpty()) return null;
        BigDecimal sum = values.stream().map(PaymentTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
