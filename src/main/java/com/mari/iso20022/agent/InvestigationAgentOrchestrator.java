package com.mari.iso20022.agent;

import com.mari.iso20022.agent.evidence.AgentPaymentSummary;
import com.mari.iso20022.agent.evidence.EvidenceAgent;
import com.mari.iso20022.agent.evidence.EvidenceAgentRequest;
import com.mari.iso20022.agent.evidence.EvidenceAgentResponse;
import com.mari.iso20022.agent.risk.RiskAgent;
import com.mari.iso20022.agent.risk.RiskAgentRequest;
import com.mari.iso20022.agent.risk.RiskAgentResponse;
import com.mari.iso20022.agent.reasoning.ReasoningAgent;
import com.mari.iso20022.agent.reasoning.ReasoningAgentRequest;
import com.mari.iso20022.agent.reasoning.ReasoningAgentResponse;
import com.mari.iso20022.agent.reasoning.ReasoningPaymentSummary;
import com.mari.iso20022.agent.verification.VerificationAgent;
import com.mari.iso20022.agent.verification.VerificationAgentRequest;
import com.mari.iso20022.agent.verification.VerificationAgentResponse;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.InvestigationEvidenceResponse;
import com.mari.iso20022.validation.ValidationFinding;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvestigationAgentOrchestrator {
    private final EvidenceAgent evidenceAgent;
    private final RiskAgent riskAgent;
    private final ReasoningAgent reasoningAgent;
    private final VerificationAgent verificationAgent;

    public InvestigationAgentOrchestrator(EvidenceAgent evidenceAgent, RiskAgent riskAgent,
                                          ReasoningAgent reasoningAgent, VerificationAgent verificationAgent) {
        this.evidenceAgent = evidenceAgent;
        this.riskAgent = riskAgent;
        this.reasoningAgent = reasoningAgent;
        this.verificationAgent = verificationAgent;
    }

    public AgentInvestigationResult assess(String transactionId, BigDecimal amount, String currency,
                                           String creditorIban, List<ValidationFinding> findings,
                                           HistoricalAnalysisResult historicalAnalysis,
                                           List<InvestigationEvidenceResponse> persistedEvidence) {
        EvidenceAgentResponse evidence = evidenceAgent.analyze(new EvidenceAgentRequest(
                new AgentPaymentSummary(transactionId, amount, currency, creditorIban),
                findings, historicalAnalysis, persistedEvidence));
        RiskAgentResponse risk = riskAgent.assess(new RiskAgentRequest(transactionId, amount, currency,
                findings, evidence, historicalAnalysis));
        return new AgentInvestigationResult(transactionId, evidence, risk);
    }

    public AgentInvestigationResult assess(com.mari.iso20022.dto.Iso20022Payment payment,
                                           List<ValidationFinding> findings,
                                           HistoricalAnalysisResult historicalAnalysis,
                                           List<InvestigationEvidenceResponse> persistedEvidence) {
        EvidenceAgentResponse evidence = evidenceAgent.analyze(new EvidenceAgentRequest(
                new AgentPaymentSummary(payment.transactionId(), payment.amount(), payment.currency(), payment.creditorIban()),
                findings, historicalAnalysis, persistedEvidence));
        RiskAgentResponse risk = riskAgent.assess(new RiskAgentRequest(payment.transactionId(), payment.amount(),
                payment.currency(), findings, evidence, historicalAnalysis));
        ReasoningPaymentSummary reasoningPayment = ReasoningPaymentSummary.from(payment);
        ReasoningAgentResponse reasoning = reasoningAgent.reason(new ReasoningAgentRequest(payment.transactionId(),
                reasoningPayment, findings, historicalAnalysis, evidence, risk.assessment(),
                evidence.evidenceItems()));
        VerificationAgentResponse verification = verificationAgent.verify(new VerificationAgentRequest(
                payment.transactionId(), reasoning, evidence.evidenceItems(), evidence, risk.assessment(), findings,
                historicalAnalysis, reasoningPayment));
        return new AgentInvestigationResult(payment.transactionId(), evidence, risk, reasoning, verification);
    }
}
