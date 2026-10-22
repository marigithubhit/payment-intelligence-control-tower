package com.mari.iso20022.investigation;

import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.investigation.dto.CollectedEvidence;
import com.mari.iso20022.investigation.dto.EvidenceType;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.HistoricalBeneficiarySummary;
import com.mari.iso20022.validation.ValidationFinding;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvidenceCollectionService {
    private static final String SOURCE = "PaymentTransactionRepository";

    public List<CollectedEvidence> collect(PaymentTransaction current, HistoricalAnalysisResult history,
                                           List<ValidationFinding> findings) {
        List<CollectedEvidence> result = new ArrayList<>();
        if (history.summary().transactionCount() == 0) return List.of();
        String ids = String.join(", ", history.historicalTransactionIds().stream().limit(10).toList());
        result.add(new CollectedEvidence(EvidenceType.HISTORICAL_TRANSACTION, "PAYMENT_HISTORY", null, null,
                "Observed " + history.summary().transactionCount() + " prior debtor transactions within the configured window. Transaction IDs: " + ids,
                SOURCE, "successful=" + history.summary().successfulTransactionCount() + "; failed=" + history.summary().failedTransactionCount(),
                "Window: " + history.analysisWindowStart() + " through " + history.analysisWindowEnd()));

        if (!history.historicalBeneficiaries().isEmpty()) {
            ValidationFinding mismatch = finding(findings, "BENEFICIARY_MISMATCH");
            String accounts = String.join(", ", history.historicalBeneficiaries().stream()
                    .map(HistoricalBeneficiarySummary::creditorIban).distinct().limit(10).toList());
            result.add(new CollectedEvidence(EvidenceType.HISTORICAL_BENEFICIARY, "BENEFICIARY",
                    mismatch == null ? null : mismatch.ruleCode(), mismatch == null ? null : mismatch.severity().name(),
                    "Successful historical payments for this beneficiary name used account(s): " + accounts,
                    SOURCE, "currentCreditorIban=" + current.getCreditorIban(),
                    "historicalCreditorIban(s)=" + accounts + "; successfulTransactions=" + history.historicalBeneficiaries().stream()
                            .mapToLong(HistoricalBeneficiarySummary::successfulTransactionCount).sum()));
        }

        if (history.summary().averageAmount() != null) {
            ValidationFinding anomaly = finding(findings, "AMOUNT_ANOMALY");
            String average = history.summary().averageAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
            String min = history.summary().minimumAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
            String max = history.summary().maximumAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
            result.add(new CollectedEvidence(EvidenceType.HISTORICAL_AMOUNT, "AMOUNT",
                    anomaly == null ? null : anomaly.ruleCode(), anomaly == null ? null : anomaly.severity().name(),
                    "Historical amount baseline uses " + history.summary().amountSampleCount() + " successful same-currency payments; any flagged deviation is an amount anomaly indicator.",
                    SOURCE, current.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + current.getCurrency(),
                    "average=" + average + "; minimum=" + min + "; maximum=" + max + " " + current.getCurrency()
                            + "; ratio=" + history.amountRatio()));
        }
        if (history.recentFailureCount() > 0) {
            ValidationFinding repeatedFailure = finding(findings, "REPEATED_PAYMENT_FAILURE");
            result.add(new CollectedEvidence(EvidenceType.HISTORICAL_FAILURE_PATTERN, "PAYMENT_BEHAVIOR",
                    repeatedFailure == null ? null : repeatedFailure.ruleCode(),
                    repeatedFailure == null ? null : repeatedFailure.severity().name(),
                    "Observed recent payment outcomes for this debtor; this evidence does not identify a failure cause.",
                    SOURCE, "failures=" + history.recentFailureCount() + "; successes=" + history.recentSuccessCount()
                            + "; total=" + history.totalRecentTransactions() + "; failureRate=" + history.failureRate() + "%",
                    "Window: " + history.analysisWindowStart() + " through " + history.analysisWindowEnd()));
        }
        return List.copyOf(result);
    }

    private static ValidationFinding finding(List<ValidationFinding> findings, String code) {
        return findings.stream().filter(f -> f.ruleCode().equals(code)).findFirst().orElse(null);
    }
}
