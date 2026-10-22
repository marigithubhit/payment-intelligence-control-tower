package com.mari.iso20022.agent.reasoning;

import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.evidence.EvidenceCompleteness;
import com.mari.iso20022.agent.evidence.EvidenceQuality;
import com.mari.iso20022.agent.risk.RiskAssessment;
import com.mari.iso20022.investigation.dto.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ReasoningAgentControllerTest {
    @Test
    void mapsJsonContractAndDelegatesToReasoningAgentWithoutWeakeningRisk() throws Exception {
        AtomicReference<ReasoningAgentRequest> delegated = new AtomicReference<>();
        ReasoningAgent agent = request -> {
            delegated.set(request);
            return ReasoningAgentResponse.unavailable(request.transactionId(), "test", "1.0", 0,
                    "LLM reasoning service is unavailable");
        };
        var controller = new ReasoningAgentController(agent);
        var request = new ReasoningAgentController.Request("TXN100003",
                new ReasoningAgentController.PaymentSummary(new BigDecimal("25000.00"), "AED", "ABC Trading LLC",
                        "AE070331234567890123456", "XYZ Supplies LLC", "AE999999987654321098765"),
                List.of(new ReasoningAgentController.Finding("BENEFICIARY_MISMATCH", "HIGH", "Beneficiary differs.")),
                new ReasoningAgentController.HistoricalAnalysis(3, "AE120331987654321098765"),
                new ReasoningAgentController.EvidenceAssessment("STRONG", "COMPLETE", new BigDecimal("0.92")),
                new ReasoningAgentController.RiskInput("HIGH", 50, new BigDecimal("0.92")),
                List.of(new ReasoningAgentController.EvidenceItem("HISTORICAL_BENEFICIARY", "Previous beneficiary.")));

        var response = controller.reason(request);

        assertEquals(ReasoningStatus.UNAVAILABLE, response.reasoningStatus());
        assertEquals("TXN100003", delegated.get().transactionId());
        assertEquals(RiskLevel.HIGH, delegated.get().riskAssessment().riskLevel());
        assertEquals("MANUAL_REVIEW", delegated.get().riskAssessment().recommendedAction());
        assertEquals("/api/v1/agents/reasoning", ReasoningAgentController.class
                .getAnnotation(RequestMapping.class).value()[0]);
        assertTrue(ReasoningAgentController.class.getMethod("reason", ReasoningAgentController.Request.class)
                .isAnnotationPresent(PostMapping.class));
    }
}
