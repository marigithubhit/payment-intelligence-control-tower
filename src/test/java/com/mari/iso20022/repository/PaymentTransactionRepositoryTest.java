package com.mari.iso20022.repository;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.parser.Pacs008Parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentTransactionRepositoryTest {
    @Autowired private PaymentTransactionRepository repository;
    @Autowired private Pacs008Parser parser;

    @BeforeEach
    void clearTransactions() {
        repository.deleteAll();
    }

    @Test
    void savesPaymentAndFindsItByBusinessTransactionId() throws Exception {
        PaymentTransaction saved = repository.saveAndFlush(newTransaction());

        PaymentTransaction found = repository.findByTransactionId("TXN100001").orElseThrow();
        assertNotNull(saved.getId());
        assertNotEquals("TXN100001", saved.getId().toString());
        assertEquals("TXN100001", found.getTransactionId());
        assertEquals(PaymentStatus.RECEIVED, found.getPaymentStatus());
        assertEquals(0, found.getAmount().compareTo(new java.math.BigDecimal("25000.00")));
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void databaseRejectsDuplicateBusinessTransactionId() throws Exception {
        repository.saveAndFlush(newTransaction());
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(newTransaction()));
    }

    private PaymentTransaction newTransaction() throws Exception {
        String xml = new String(getClass().getResourceAsStream("/iso20022/pacs008/sample-pacs008-payment.xml")
                .readAllBytes(), StandardCharsets.UTF_8);
        Iso20022Payment payment = parser.parse(xml);
        return PaymentTransaction.received(payment);
    }
}
