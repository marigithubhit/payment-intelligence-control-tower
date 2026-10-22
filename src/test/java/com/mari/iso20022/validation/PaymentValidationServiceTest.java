package com.mari.iso20022.validation;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.parser.Pacs008Parser;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentValidationServiceTest {
    @Autowired private PaymentValidationService validationService;
    @Autowired private PaymentTransactionRepository repository;
    @Autowired private Pacs008Parser parser;
    @Autowired private JdbcTemplate jdbcTemplate;
    private Iso20022Payment sample;

    @BeforeEach
    void setUp() throws Exception {
        repository.deleteAll();
        sample = readSample();
    }

    @Test
    void reportsMissingRequiredFieldsAndInvalidAmount() {
        Iso20022Payment invalid = copy(sample, null, null, "", "bad-account", "bad-account", null);
        List<String> codes = validationService.validate(invalid).findings().stream()
                .map(ValidationFinding::ruleCode).toList();
        assertTrue(codes.contains("MISSING_REQUIRED_FIELD"));
        assertTrue(codes.contains("INVALID_AMOUNT"));
        assertTrue(codes.contains("INVALID_ACCOUNT_FORMAT"));
    }

    @Test
    void reportsUnsupportedCurrencyAndMalformedAccount() {
        Iso20022Payment invalid = copy(sample, new BigDecimal("50.00"), "JPY", sample.creditorName(),
                "bad-iban", sample.creditorIban(), sample.transactionId());
        List<String> codes = validationService.validate(invalid).findings().stream()
                .map(ValidationFinding::ruleCode).toList();
        assertTrue(codes.contains("UNSUPPORTED_CURRENCY"));
        assertTrue(codes.contains("INVALID_ACCOUNT_FORMAT"));
    }

    @Test
    void detectsDuplicateUsingPersistedBusinessTransactionId() {
        repository.saveAndFlush(PaymentTransaction.received(sample));
        assertTrue(validationService.validate(sample).findings().stream()
                .anyMatch(finding -> finding.ruleCode().equals("DUPLICATE_TRANSACTION")));
    }

    @Test
    void doesNotInventBeneficiaryMismatchWithoutHistory() {
        assertFalse(validationService.validate(sample).findings().stream()
                .anyMatch(finding -> finding.ruleCode().equals("BENEFICIARY_MISMATCH")));
    }

    @Test
    void matchingSuccessfulBeneficiaryAccountDoesNotTriggerMismatch() {
        saveHistory(copy(sample, new BigDecimal("4000.00"), "AED", sample.creditorName(),
                sample.debtorIban(), sample.creditorIban(), "TXN099990"), PaymentStatus.SUCCESS);
        assertFalse(validationService.validate(sample).findings().stream()
                .anyMatch(finding -> finding.ruleCode().equals("BENEFICIARY_MISMATCH")));
    }

    @Test
    void oneMatchingSuccessfulAccountIsEnoughToAvoidMismatch() {
        saveHistory(copy(sample, new BigDecimal("4000.00"), "AED", sample.creditorName(),
                sample.debtorIban(), "AE980331111122223333444", "TXN099991"), PaymentStatus.SUCCESS);
        saveHistory(copy(sample, new BigDecimal("4500.00"), "AED", sample.creditorName(),
                sample.debtorIban(), sample.creditorIban(), "TXN099992"), PaymentStatus.SUCCESS);
        assertFalse(validationService.validate(sample).findings().stream()
                .anyMatch(finding -> finding.ruleCode().equals("BENEFICIARY_MISMATCH")));
    }

    @Test
    void detectsBeneficiaryMismatchWithSuccessfulPaymentEvidence() {
        saveHistory(copy(sample, new BigDecimal("4000.00"), "AED", sample.creditorName(),
                sample.debtorIban(), "AE980331111122223333444", "TXN099991"), PaymentStatus.SUCCESS);
        PaymentValidationResult result = validationService.validate(sample);
        ValidationFinding finding = result.findings().stream()
                .filter(value -> value.ruleCode().equals("BENEFICIARY_MISMATCH")).findFirst().orElseThrow();
        assertEquals("HIGH", finding.severity().name());
        assertTrue(finding.evidence().contains("TXN099991"));
        assertEquals("TXN099991", result.historicalEvidence().getFirst().transactionId());
    }

    @Test
    void detectsRepeatedFailuresAtConfiguredThreshold() {
        for (int index = 1; index <= 3; index++) {
            saveHistory(copy(sample, new BigDecimal("100.00"), "AED", sample.creditorName(),
                    sample.debtorIban(), sample.creditorIban(), "TXNFAIL00" + index), PaymentStatus.FAILED);
        }
        ValidationFinding finding = validationService.validate(sample).findings().stream()
                .filter(value -> value.ruleCode().equals("REPEATED_PAYMENT_FAILURE")).findFirst().orElseThrow();
        assertEquals(ValidationSeverity.MEDIUM, finding.severity());
        assertTrue(finding.evidence().contains("3 failed transactions"));
    }

    @Test
    void detectsAmountAboveConfiguredHistoricalMultiplier() {
        for (int index = 1; index <= 3; index++) {
            saveHistory(copy(sample, new BigDecimal("1000.00"), "AED", sample.creditorName(),
                    sample.debtorIban(), sample.creditorIban(), "TXNAVG000" + index), PaymentStatus.SUCCESS);
        }
        Iso20022Payment anomalous = copy(sample, new BigDecimal("4000.00"), "AED", sample.creditorName(),
                sample.debtorIban(), sample.creditorIban(), sample.transactionId());
        ValidationFinding finding = validationService.validate(anomalous).findings().stream()
                .filter(value -> value.ruleCode().equals("AMOUNT_ANOMALY")).findFirst().orElseThrow();
        assertEquals(ValidationSeverity.MEDIUM, finding.severity());
        assertTrue(finding.evidence().contains("3.0x historical average"));
    }

    private void saveHistory(Iso20022Payment payment, PaymentStatus status) {
        PaymentTransaction saved = repository.saveAndFlush(PaymentTransaction.received(payment));
        jdbcTemplate.update("UPDATE payment_transaction SET payment_status = ? WHERE id = ?", status.name(), saved.getId());
    }

    private Iso20022Payment readSample() throws Exception {
        String xml = new String(getClass().getResourceAsStream("/iso20022/pacs008/sample-pacs008-payment.xml")
                .readAllBytes(), StandardCharsets.UTF_8);
        return parser.parse(xml);
    }

    private Iso20022Payment copy(Iso20022Payment source, BigDecimal amount, String currency, String creditorName,
                                 String debtorIban, String creditorIban, String transactionId) {
        return new Iso20022Payment(source.messageId(), source.creationDateTime(), source.numberOfTransactions(),
                source.settlementMethod(), source.instructionId(), source.endToEndId(), transactionId, amount,
                currency, source.settlementDate(), source.debtorName(), debtorIban, source.debtorBic(),
                creditorName, creditorIban, source.creditorBic(), source.remittanceInformation());
    }
}
