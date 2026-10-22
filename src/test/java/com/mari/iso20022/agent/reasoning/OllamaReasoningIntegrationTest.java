package com.mari.iso20022.agent.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.evidence.EvidenceCompleteness;
import com.mari.iso20022.agent.evidence.EvidenceQuality;
import com.mari.iso20022.agent.risk.RiskAssessment;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OllamaReasoningIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "ollama.integration", matches = "true")
    void localOllamaReturnsValidatedReasoning() {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var ollama = new OllamaProperties();
        var agent = new OllamaReasoningAgent(new OllamaGenerateClient(ollama, mapper), mapper,
                new ReasoningPromptBuilder(mapper), ollama, new ReasoningProperties());
        var evidence = new EvidenceAgentResponse("OLLAMA-INTEGRATION", List.of(), EvidenceQuality.INSUFFICIENT,
                EvidenceCompleteness.INSUFFICIENT, BigDecimal.ZERO, List.of("No supporting evidence was supplied."),
                false, "Insufficient evidence", 0);
        var risk = new RiskAssessment("OLLAMA-INTEGRATION", 0, RiskLevel.LOW, BigDecimal.ZERO,
                List.of(), "REVIEW", true, "Review due to insufficient evidence");
        var history = new HistoricalAnalysisResult("OLLAMA-INTEGRATION", 90, null, null, null,
                BigDecimal.ONE, "AED", null, List.of(), 0, 0, 0, BigDecimal.ZERO, null, List.of());
        var response = agent.reason(new ReasoningAgentRequest("OLLAMA-INTEGRATION", null, List.of(), history,
                evidence, risk, List.of()));
        assertEquals(ReasoningStatus.AVAILABLE, response.reasoningStatus(), response.reasoningError());
    }
}
