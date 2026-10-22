package com.mari.iso20022.agent.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.evidence.EvidenceCompleteness;
import com.mari.iso20022.agent.evidence.EvidenceQuality;
import com.mari.iso20022.agent.risk.RiskAssessment;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.RiskLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OllamaReasoningAgentTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final OllamaProperties ollama = new OllamaProperties();
    private final ReasoningProperties props = new ReasoningProperties();

    @Test void acceptsGroundedJsonAndCapsInsufficientEvidenceConfidence() {
        var response = agent("{\"transactionId\":\"T1\",\"summary\":\"Evidence is insufficient.\",\"observedFacts\":[],\"evidenceAssessment\":[],\"reasoning\":[],\"uncertainties\":[],\"summaryEvidenceIds\":[],\"recommendedAnalystAction\":\"REVIEW\",\"confidence\":0.9}").reason(request(EvidenceQuality.INSUFFICIENT));
        assertEquals(ReasoningStatus.AVAILABLE, response.reasoningStatus(), response.reasoningError());
        assertEquals(new BigDecimal("0.50"), response.reasoningConfidence());
        assertTrue(response.confidenceCapped());
    }

    @Test void rejectsUnexpectedProperties() {
        var response = agent("{\"transactionId\":\"T1\",\"summary\":\"ok\",\"observedFacts\":[],\"evidenceAssessment\":[],\"reasoning\":[],\"uncertainties\":[],\"summaryEvidenceIds\":[],\"recommendedAnalystAction\":\"NO_ACTION\",\"confidence\":0.2,\"extra\":true}").reason(request(EvidenceQuality.STRONG));
        assertEquals(ReasoningStatus.REJECTED, response.reasoningStatus());
    }

    @Test void reportsUnavailableWithoutFailingDeterministicResult() {
        OllamaClient client = prompt -> { throw new OllamaCallException("offline", new RuntimeException()); };
        var response = agent(client).reason(request(EvidenceQuality.STRONG));
        assertEquals(ReasoningStatus.UNAVAILABLE, response.reasoningStatus());
        assertTrue(response.deterministicInvestigationAvailable());
    }

    @Test void rejectsEmptyModelResponse() {
        var response = agent("").reason(request(EvidenceQuality.STRONG));
        assertEquals(ReasoningStatus.REJECTED, response.reasoningStatus());
    }

    @Test void rejectsTransactionIdMismatch() {
        var response = agent("{\"transactionId\":\"OTHER\",\"summary\":\"Evidence is insufficient.\",\"observedFacts\":[],\"evidenceAssessment\":[],\"reasoning\":[],\"uncertainties\":[],\"summaryEvidenceIds\":[],\"recommendedAnalystAction\":\"REVIEW\",\"confidence\":0.8}").reason(request(EvidenceQuality.STRONG));
        assertEquals(ReasoningStatus.REJECTED, response.reasoningStatus());
    }

    @Test void rejectsRecommendationThatDowngradesHighDeterministicRisk() {
        var q = request(EvidenceQuality.STRONG);
        var highRisk = new RiskAssessment("T1", 50, RiskLevel.HIGH, new BigDecimal("0.92"), List.of(),
                "MANUAL_REVIEW", true, "High deterministic risk");
        q = new ReasoningAgentRequest(q.transactionId(), q.paymentSummary(), q.validationFindings(),
                q.historicalAnalysis(), q.evidenceAssessment(), highRisk, q.evidenceItems());
        var response = agent("{\"transactionId\":\"T1\",\"summary\":\"Evidence is insufficient.\",\"observedFacts\":[],\"evidenceAssessment\":[],\"reasoning\":[],\"uncertainties\":[],\"summaryEvidenceIds\":[],\"recommendedAnalystAction\":\"MONITOR\",\"confidence\":0.8}").reason(q);
        assertEquals(ReasoningStatus.REJECTED, response.reasoningStatus());
    }

    private OllamaReasoningAgent agent(String output) {
        return agent(prompt -> output);
    }

    private OllamaReasoningAgent agent(OllamaClient client) {
        return new OllamaReasoningAgent(client, mapper, new ReasoningPromptBuilder(mapper), ollama, props);
    }

    private ReasoningAgentRequest request(EvidenceQuality quality) {
        var evidence = new EvidenceAgentResponse("T1", List.of(), quality,
                quality == EvidenceQuality.INSUFFICIENT ? EvidenceCompleteness.INSUFFICIENT : EvidenceCompleteness.COMPLETE,
                new BigDecimal("0.8"), List.of(), false, "test", 0);
        var risk = new RiskAssessment("T1", 0, RiskLevel.LOW, new BigDecimal("0.8"), List.of(), "REVIEW", true, "test");
        HistoricalAnalysisResult history = new HistoricalAnalysisResult("T1", 90, null, null, null,
                BigDecimal.ONE, "AED", null, List.of(), 0, 0, 0, BigDecimal.ZERO, null, List.of());
        return new ReasoningAgentRequest("T1", null, List.of(), history, evidence, risk, List.of());
    }
}
