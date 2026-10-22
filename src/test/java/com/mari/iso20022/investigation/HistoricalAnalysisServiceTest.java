package com.mari.iso20022.investigation;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.parser.Pacs008Parser;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HistoricalAnalysisServiceTest {
    @Autowired private HistoricalAnalysisService analysisService;
    @Autowired private PaymentTransactionRepository repository;
    @Autowired private Pacs008Parser parser;
    @Autowired private JdbcTemplate jdbcTemplate;
    private Iso20022Payment sample;

    @BeforeEach
    void setUp() throws Exception {
        repository.deleteAll();
        sample = parser.parse(new String(new ClassPathResource("iso20022/pacs008/sample-pacs008-payment.xml")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void summarizesPersistedSuccessfulHistoryAndExcludesCurrentAndOtherCurrencies() {
        save(payment("HIST000001", new BigDecimal("20000"), "AED", sample.creationDateTime().minusDays(10)), PaymentStatus.SUCCESS);
        save(payment("HIST000002", new BigDecimal("22000"), "AED", sample.creationDateTime().minusDays(5)), PaymentStatus.SUCCESS);
        save(payment("HIST000003", new BigDecimal("99000"), "USD", sample.creationDateTime().minusDays(2)), PaymentStatus.SUCCESS);
        PaymentTransaction current = repository.saveAndFlush(PaymentTransaction.received(sample));

        var result = analysisService.analyze(current);
        assertEquals(3, result.summary().transactionCount());
        assertEquals(3, result.summary().successfulTransactionCount());
        assertEquals(2, result.summary().amountSampleCount());
        assertEquals(new BigDecimal("21000.00"), result.summary().averageAmount());
        assertEquals(0, result.summary().minimumAmount().compareTo(new BigDecimal("20000")));
        assertEquals(0, result.summary().maximumAmount().compareTo(new BigDecimal("22000")));
        assertEquals(new BigDecimal("1.19"), result.amountRatio());
        assertEquals(3, result.historicalBeneficiaries().getFirst().successfulTransactionCount());
        assertFalse(result.historicalTransactionIds().contains(current.getTransactionId()));
    }

    @Test
    void excludesTransactionsOutsideNinetyDayWindowAndCountsRecentFailuresFactually() {
        save(payment("HISTOLD0001", new BigDecimal("100"), "AED", sample.creationDateTime().minusDays(91)), PaymentStatus.FAILED);
        save(payment("HISTFAIL001", new BigDecimal("100"), "AED", sample.creationDateTime().minusDays(20)), PaymentStatus.FAILED);
        save(payment("HISTSUCCESS1", new BigDecimal("100"), "AED", sample.creationDateTime().minusDays(10)), PaymentStatus.SUCCESS);
        PaymentTransaction current = repository.saveAndFlush(PaymentTransaction.received(sample));

        var result = analysisService.analyze(current);
        assertEquals(2, result.summary().transactionCount());
        assertEquals(1, result.recentFailureCount());
        assertEquals(1, result.recentSuccessCount());
        assertEquals(2, result.totalRecentTransactions());
        assertEquals(new BigDecimal("50.00"), result.failureRate());
        assertEquals(OffsetDateTime.parse(sample.creationDateTime().minusDays(20).toString()), result.mostRecentFailureDate());
    }

    @Test
    void noHistoryProducesEmptySummaryAndNoDerivedAmountValues() {
        PaymentTransaction current = repository.saveAndFlush(PaymentTransaction.received(sample));
        var result = analysisService.analyze(current);
        assertEquals(0, result.summary().transactionCount());
        assertEquals(0, result.summary().successfulTransactionCount());
        assertNull(result.summary().averageAmount());
        assertNull(result.amountRatio());
        assertTrue(result.historicalBeneficiaries().isEmpty());
        assertTrue(result.historicalTransactionIds().isEmpty());
        assertNull(result.failureRate());
    }

    @Test
    void includesTransactionAtInclusiveNinetyDayBoundary() {
        save(payment("HISTBOUND001", new BigDecimal("200"), "AED", sample.creationDateTime().minusDays(90)), PaymentStatus.SUCCESS);
        PaymentTransaction current = repository.saveAndFlush(PaymentTransaction.received(sample));
        assertEquals(1, analysisService.analyze(current).summary().transactionCount());
    }

    @Test
    void sameBeneficiarySummaryRequiresSuccessfulTransactions() {
        save(payment("HISTFAIL002", new BigDecimal("20000"), "AED", sample.creationDateTime().minusDays(3)), PaymentStatus.FAILED);
        PaymentTransaction current = repository.saveAndFlush(PaymentTransaction.received(sample));
        var result = analysisService.analyze(current);
        assertTrue(result.historicalBeneficiaries().isEmpty());
        assertNull(result.summary().averageAmount());
    }

    private void save(Iso20022Payment payment, PaymentStatus status) {
        PaymentTransaction transaction = repository.saveAndFlush(PaymentTransaction.received(payment));
        if (status != PaymentStatus.RECEIVED) {
            jdbcTemplate.update("UPDATE payment_transaction SET payment_status = ? WHERE id = ?", status.name(), transaction.getId());
        }
    }

    private Iso20022Payment payment(String id, BigDecimal amount, String currency, OffsetDateTime created) {
        return new Iso20022Payment(sample.messageId(), created, sample.numberOfTransactions(), sample.settlementMethod(),
                sample.instructionId(), sample.endToEndId(), id, amount, currency, sample.settlementDate(),
                sample.debtorName(), sample.debtorIban(), sample.debtorBic(), sample.creditorName(),
                sample.creditorIban(), sample.creditorBic(), sample.remittanceInformation());
    }
}
