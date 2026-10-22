package com.mari.iso20022.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mari.iso20022.investigation.dto.InvestigationResult;
import com.mari.iso20022.investigation.dto.InvestigationStatus;
import com.mari.iso20022.investigation.repository.InvestigationCaseRepository;
import com.mari.iso20022.investigation.repository.InvestigationReviewRepository;
import com.mari.iso20022.investigation.dto.RiskLevel;
import com.mari.iso20022.investigation.entity.InvestigationCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HumanReviewControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private InvestigationCaseRepository caseRepository;
    @Autowired private InvestigationReviewRepository reviewRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        reviewRepository.deleteAll();
        caseRepository.deleteAll();
    }

    @Test
    void recordsApprovalAndPreservesDeterministicRiskAndSummary() throws Exception {
        UUID caseId = createCase("TXN-HUMAN-APPROVE", InvestigationStatus.REQUIRES_REVIEW, RiskLevel.HIGH);

        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("APPROVE", "Reviewed evidence and approved payment.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(caseId.toString()))
                .andExpect(jsonPath("$.transactionId").value("TXN-HUMAN-APPROVE"))
                .andExpect(jsonPath("$.decision").value("APPROVE"))
                .andExpect(jsonPath("$.reviewer").value("demo-investigator"))
                .andExpect(jsonPath("$.previousStatus").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$.newStatus").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        InvestigationCase updated = caseRepository.findById(caseId).orElseThrow();
        assertEquals("APPROVED", updated.getStatus());
        assertEquals("HIGH", updated.getRiskLevel());
        assertEquals("MANUAL_REVIEW", updated.getRecommendation());
        assertEquals(1, reviewRepository.count());
    }

    @Test
    void recordsRejection() throws Exception {
        UUID caseId = createCase("TXN-HUMAN-REJECT", InvestigationStatus.REQUIRES_REVIEW, RiskLevel.HIGH);
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("REJECT", "Evidence does not support release.")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.newStatus").value("REJECTED"));
    }

    @Test
    void recordsEscalation() throws Exception {
        UUID caseId = createCase("TXN-HUMAN-ESCALATE", InvestigationStatus.REQUIRES_REVIEW, RiskLevel.CRITICAL);
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("ESCALATE", "Evidence conflict needs further review.")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.newStatus").value("ESCALATED"));
    }

    @Test
    void rejectsMissingOrBlankReasonForEveryDecision() throws Exception {
        for (String decision : List.of("APPROVE", "REJECT", "ESCALATE")) {
            mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", UUID.randomUUID())
                            .contentType(APPLICATION_JSON)
                            .content("{\"decision\":\"" + decision + "\",\"reviewer\":\"demo-investigator\",\"reason\":\" \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REVIEW_REQUEST"));
        }
    }

    @Test
    void reportsUnknownCaseForSubmissionAndHistory() throws Exception {
        UUID missingCase = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", missingCase)
                        .contentType(APPLICATION_JSON).content(request("APPROVE", "Reviewed evidence and approved payment.")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVESTIGATION_CASE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/investigations/cases/{caseId}/reviews", missingCase))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnotherDecisionAfterFinalApproval() throws Exception {
        UUID caseId = createCase("TXN-HUMAN-TRANSITION", InvestigationStatus.REQUIRES_REVIEW, RiskLevel.HIGH);
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("APPROVE", "Reviewed evidence and approved payment.")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("REJECT", "Changed decision after approval.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_TRANSITION"));
        assertEquals(1, reviewRepository.count());
    }

    @Test
    void rejectsReviewForLowRiskClearCaseWithoutHumanReviewFlag() throws Exception {
        UUID caseId = createCase("TXN-HUMAN-NOT-REQUIRED", InvestigationStatus.CLEAR, RiskLevel.LOW);
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("APPROVE", "Reviewed evidence and approved payment.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_TRANSITION"));
        assertEquals(0, reviewRepository.count());
    }

    @Test
    void rejectsDecisionOnClosedCase() throws Exception {
        UUID caseId = createCase("TXN-HUMAN-CLOSED", InvestigationStatus.REQUIRES_REVIEW, RiskLevel.HIGH);
        jdbcTemplate.update("UPDATE investigation_case SET status = 'CLOSED' WHERE id = ?", caseId);
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("APPROVE", "Reviewed evidence and approved payment.")))
                .andExpect(status().isConflict());
        assertEquals(0, reviewRepository.count());
    }

    @Test
    void persistsAppendOnlyHistoryInChronologicalOrder() throws Exception {
        UUID caseId = createCase("TXN-HUMAN-HISTORY", InvestigationStatus.REQUIRES_REVIEW, RiskLevel.HIGH);
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("ESCALATE", "Evidence conflict needs further review.")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/investigations/cases/{caseId}/review", caseId)
                        .contentType(APPLICATION_JSON).content(request("APPROVE", "Additional review completed and approved.")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/investigations/cases/{caseId}/reviews", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].decision").value("ESCALATE"))
                .andExpect(jsonPath("$[0].previousStatus").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$[0].newStatus").value("ESCALATED"))
                .andExpect(jsonPath("$[1].decision").value("APPROVE"))
                .andExpect(jsonPath("$[1].previousStatus").value("ESCALATED"))
                .andExpect(jsonPath("$[1].newStatus").value("APPROVED"));
        assertEquals(2, reviewRepository.count());
        assertNotEquals(reviewRepository.findAll().get(0).getId(), reviewRepository.findAll().get(1).getId());
    }

    private UUID createCase(String transactionId, InvestigationStatus status, RiskLevel riskLevel) {
        InvestigationResult result = new InvestigationResult(transactionId, status, riskLevel,
                List.of(), List.of(), "MANUAL_REVIEW", null, null, null, null);
        return caseRepository.saveAndFlush(InvestigationCase.from(result, "Synthetic test case requiring review.")).getId();
    }

    private String request(String decision, String reason) throws Exception {
        return objectMapper.writeValueAsString(new Request(decision, reason, "demo-investigator"));
    }

    private record Request(String decision, String reason, String reviewer) { }
}
