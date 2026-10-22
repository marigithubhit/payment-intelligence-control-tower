package com.mari.iso20022.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;
import com.mari.iso20022.repository.PaymentTransactionRepository;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIngestionControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentTransactionRepository repository;

    @BeforeEach
    void clearTransactions() {
        repository.deleteAll();
    }

    @Test
    void ingestsSamplePacs008AndReturnsStructuredJson() throws Exception {
        String xml = new String(new ClassPathResource("iso20022/pacs008/sample-pacs008-payment.xml")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        mockMvc.perform(post("/api/v1/payments/iso20022").contentType(MediaType.APPLICATION_XML).content(xml))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.messageId").value("MARI202610060001"))
                .andExpect(jsonPath("$.transactionId").value("TXN100001"))
                .andExpect(jsonPath("$.amount").value(25000.00))
                .andExpect(jsonPath("$.currency").value("AED"))
                .andExpect(jsonPath("$.debtor").value("ABC Trading LLC"))
                .andExpect(jsonPath("$.creditor").value("XYZ Supplies LLC"))
                .andExpect(jsonPath("$.persisted").value(true));
        org.junit.jupiter.api.Assertions.assertTrue(repository.existsByTransactionId("TXN100001"));
    }

    @Test
    void rejectsMalformedXmlWithCleanJsonError() throws Exception {
        mockMvc.perform(post("/api/v1/payments/iso20022").contentType(MediaType.APPLICATION_XML).content("<Document>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ISO20022_MESSAGE"))
                .andExpect(jsonPath("$.message").value("Unable to parse ISO 20022 payment message"));
        org.junit.jupiter.api.Assertions.assertEquals(0, repository.count());
    }

    @Test
    void rejectsDuplicateBusinessTransactionId() throws Exception {
        String xml = new String(new ClassPathResource("iso20022/pacs008/sample-pacs008-payment.xml")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        mockMvc.perform(post("/api/v1/payments/iso20022").contentType(MediaType.APPLICATION_XML).content(xml))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payments/iso20022").contentType(MediaType.APPLICATION_XML).content(xml))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TRANSACTION"));
        org.junit.jupiter.api.Assertions.assertEquals(1, repository.count());
    }
}
