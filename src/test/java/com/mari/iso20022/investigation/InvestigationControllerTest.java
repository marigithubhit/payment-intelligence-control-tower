package com.mari.iso20022.investigation;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.parser.Pacs008Parser;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import com.mari.iso20022.investigation.repository.InvestigationCaseRepository;
import com.mari.iso20022.investigation.repository.InvestigationEvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvestigationControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentTransactionRepository repository;
    @Autowired private InvestigationCaseRepository caseRepository;
    @Autowired private InvestigationEvidenceRepository evidenceRepository;
    @Autowired private Pacs008Parser parser;
    @Autowired private JdbcTemplate jdbcTemplate;
    private Iso20022Payment sample;

    @BeforeEach
    void setUp() throws Exception {
        caseRepository.deleteAll();
        repository.deleteAll();
        sample = parser.parse(new String(new ClassPathResource("iso20022/pacs008/sample-pacs008-payment.xml")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void returnsClearWhenThereIsNoAdverseEvidence() throws Exception {
        save(sample, PaymentStatus.SUCCESS);
        mockMvc.perform(get("/api/v1/investigations/TXN100001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEAR"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.recommendation").value("NO_ACTION"))
                .andExpect(jsonPath("$.findings").isEmpty())
                .andExpect(jsonPath("$.historicalEvidence").isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(1,
                caseRepository.findByTransactionIdOrderByCreatedAtDesc("TXN100001").size());
        org.junit.jupiter.api.Assertions.assertEquals(0, evidenceRepository.count());
    }

    @Test
    void returnsEvidenceBackedBeneficiaryMismatchAndManualReview() throws Exception {
        save(copy("TXN099991", new BigDecimal("4000.00"), "AE980331111122223333444"), PaymentStatus.SUCCESS);
        save(copy("TXN100003", sample.amount(), sample.creditorIban()), PaymentStatus.RECEIVED);
        MvcResult result = mockMvc.perform(get("/api/v1/investigations/TXN100003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.recommendation").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='BENEFICIARY_MISMATCH')].passed").value(false))
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='BENEFICIARY_MISMATCH')].evidence").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("TXN099991"))))
                .andExpect(jsonPath("$.historicalEvidence[0].transactionId").value("TXN099991"))
                .andReturn();
        String caseId = JsonPath.read(result.getResponse().getContentAsString(), "$.caseId");
        mockMvc.perform(get("/api/v1/investigations/cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(caseId))
                .andExpect(jsonPath("$.transactionId").value("TXN100003"))
                .andExpect(jsonPath("$.summary").value(
                        "Payment transaction requires manual review due to one or more high-severity investigation findings."))
                .andExpect(jsonPath("$.evidence[?(@.ruleCode=='BENEFICIARY_MISMATCH')]").isNotEmpty())
                .andExpect(jsonPath("$.evidence[?(@.ruleCode=='BENEFICIARY_MISMATCH')].description").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("TXN099991"))))
                .andExpect(jsonPath("$.evidence[?(@.evidenceType=='HISTORICAL_BENEFICIARY')]").isNotEmpty())
                .andExpect(jsonPath("$.evidence[?(@.evidenceType=='HISTORICAL_BENEFICIARY')].sourceReference")
                        .value(org.hamcrest.Matchers.hasItem("PaymentTransactionRepository")))
                .andExpect(jsonPath("$.evidence[?(@.sourceReference=='PaymentTransactionRepository')]").isNotEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(evidenceRepository.count() >= 2);
    }

    @Test
    void escalatesMultipleEvidenceBackedHighFindingsToCritical() throws Exception {
        for (int index = 1; index <= 3; index++) {
            save(copy("TXNBASE00" + index, new BigDecimal("1000.00"), "AE980331111122223333444"), PaymentStatus.SUCCESS);
        }
        Iso20022Payment current = copy("TXN100007", new BigDecimal("10000.00"), sample.creditorIban());
        save(current, PaymentStatus.RECEIVED);
        mockMvc.perform(get("/api/v1/investigations/TXN100007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.recommendation").value("HOLD_AND_ESCALATE"))
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='BENEFICIARY_MISMATCH')]").isNotEmpty())
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='AMOUNT_ANOMALY')]").isNotEmpty())
                .andExpect(jsonPath("$.historicalEvidence.length()").value(3));
        org.junit.jupiter.api.Assertions.assertEquals(5, evidenceRepository.count());
    }

    @Test
    void returnsRepeatedFailureAndInvalidIbanFindings() throws Exception {
        for (int index = 1; index <= 3; index++) {
            save(copy("TXNFAIL00" + index, new BigDecimal("100.00"), sample.creditorIban()), PaymentStatus.FAILED);
        }
        Iso20022Payment current = copy("TXN100006", sample.amount(), "bad-iban");
        save(current, PaymentStatus.RECEIVED);
        mockMvc.perform(get("/api/v1/investigations/TXN100006"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='REPEATED_PAYMENT_FAILURE')]").isNotEmpty())
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='INVALID_ACCOUNT_FORMAT')]").isNotEmpty());
    }

    @Test
    void persistsRepeatedFailureCaseAndCreatesHistoryForEachInvestigation() throws Exception {
        for (int index = 1; index <= 3; index++) {
            save(copy("TXNFAIL00" + index, new BigDecimal("100.00"), sample.creditorIban()), PaymentStatus.FAILED);
        }
        save(copy("TXN100004", sample.amount(), sample.creditorIban()), PaymentStatus.RECEIVED);
        MvcResult first = mockMvc.perform(get("/api/v1/investigations/TXN100004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='REPEATED_PAYMENT_FAILURE')]").isNotEmpty())
                .andExpect(jsonPath("$.recommendation").value("REVIEW"))
                .andReturn();
        MvcResult second = mockMvc.perform(get("/api/v1/investigations/TXN100004"))
                .andExpect(status().isOk()).andReturn();
        String firstCase = JsonPath.read(first.getResponse().getContentAsString(), "$.caseId");
        String secondCase = JsonPath.read(second.getResponse().getContentAsString(), "$.caseId");
        org.junit.jupiter.api.Assertions.assertNotEquals(firstCase, secondCase);
        org.junit.jupiter.api.Assertions.assertEquals(2,
                caseRepository.findByTransactionIdOrderByCreatedAtDesc("TXN100004").size());
    }

    @Test
    void persistsAmountAnomalyCaseForTransaction100005() throws Exception {
        for (int index = 1; index <= 3; index++) {
            save(copy("TXNAVG000" + index, new BigDecimal("1000.00"), sample.creditorIban()), PaymentStatus.SUCCESS);
        }
        save(copy("TXN100005", new BigDecimal("4000.00"), sample.creditorIban()), PaymentStatus.RECEIVED);
        MvcResult result = mockMvc.perform(get("/api/v1/investigations/TXN100005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.recommendation").value("REVIEW"))
                .andExpect(jsonPath("$.findings[?(@.ruleCode=='AMOUNT_ANOMALY')]").isNotEmpty())
                .andExpect(jsonPath("$.agentAssessment.evidenceAgent.evidenceQuality").value("STRONG"))
                .andExpect(jsonPath("$.agentAssessment.evidenceAgent.evidenceItems[?(@.evidenceType=='HISTORICAL_AMOUNT')]").isNotEmpty())
                .andExpect(jsonPath("$.agentAssessment.riskAgent.assessment.riskScore").value(25))
                .andExpect(jsonPath("$.agentAssessment.riskAgent.assessment.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.agentAssessment.riskAgent.assessment.riskFactors[0].code").value("AMOUNT_ANOMALY"))
                .andReturn();
        String caseId = JsonPath.read(result.getResponse().getContentAsString(), "$.caseId");
        mockMvc.perform(get("/api/v1/investigations/cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence[?(@.evidenceType=='HISTORICAL_AMOUNT')].observedValue")
                        .value(org.hamcrest.Matchers.hasItem("4000.00 AED")))
                .andExpect(jsonPath("$.evidence[?(@.evidenceType=='HISTORICAL_AMOUNT')].expectedValue")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("average=1000.00"))));
    }

    @Test
    void returnsConsistentNotFoundError() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/DOES-NOT-EXIST").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Transaction DOES-NOT-EXIST was not found"));
    }

    @Test
    void returnsNotFoundForUnknownCaseId() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/cases/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVESTIGATION_CASE_NOT_FOUND"));
    }

    @Test
    void historyEndpointReturnsAnalysisWithoutCreatingAnInvestigationCase() throws Exception {
        save(copy("TXNHIST001", new BigDecimal("20000.00"), sample.creditorIban()), PaymentStatus.SUCCESS);
        save(copy("TXNHIST002", new BigDecimal("22000.00"), sample.creditorIban()), PaymentStatus.SUCCESS);
        save(copy("TXN100005", new BigDecimal("150000.00"), sample.creditorIban()), PaymentStatus.RECEIVED);
        mockMvc.perform(get("/api/v1/investigations/TXN100005/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.transactionCount").value(2))
                .andExpect(jsonPath("$.summary.averageAmount").value(21000.00))
                .andExpect(jsonPath("$.summary.minimumAmount").value(20000.00))
                .andExpect(jsonPath("$.summary.maximumAmount").value(22000.00))
                .andExpect(jsonPath("$.amountRatio").value(7.14))
                .andExpect(jsonPath("$.historicalTransactionIds.length()").value(2));
        org.junit.jupiter.api.Assertions.assertEquals(0, caseRepository.count());
    }

    private void save(Iso20022Payment payment, PaymentStatus status) {
        PaymentTransaction saved = repository.saveAndFlush(PaymentTransaction.received(payment));
        if (status != PaymentStatus.RECEIVED) {
            jdbcTemplate.update("UPDATE payment_transaction SET payment_status = ? WHERE id = ?", status.name(), saved.getId());
        }
    }

    private Iso20022Payment copy(String transactionId, BigDecimal amount, String creditorIban) {
        return new Iso20022Payment(sample.messageId(), sample.creationDateTime(), sample.numberOfTransactions(),
                sample.settlementMethod(), sample.instructionId(), sample.endToEndId(), transactionId, amount,
                sample.currency(), sample.settlementDate(), sample.debtorName(), sample.debtorIban(),
                sample.debtorBic(), sample.creditorName(), creditorIban, sample.creditorBic(),
                sample.remittanceInformation());
    }
}
