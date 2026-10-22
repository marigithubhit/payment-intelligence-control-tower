package com.mari.iso20022.investigation.dto;

import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.agent.AgentInvestigationResult;

import java.time.OffsetDateTime;
import java.util.List;

public record InvestigationResult(String transactionId,
                                  InvestigationStatus status,
                                  RiskLevel riskLevel,
                                  List<ValidationFinding> findings,
                                  List<HistoricalPaymentEvidence> historicalEvidence,
                                  String recommendation,
                                  String caseId,
                                  String summary,
                                  OffsetDateTime openedAt,
                                  OffsetDateTime completedAt,
                                  AgentInvestigationResult agentAssessment) {
    public InvestigationResult(String transactionId, InvestigationStatus status, RiskLevel riskLevel,
                                List<ValidationFinding> findings, List<HistoricalPaymentEvidence> historicalEvidence,
                                String recommendation, String caseId, String summary, OffsetDateTime openedAt,
                                OffsetDateTime completedAt) {
        this(transactionId, status, riskLevel, findings, historicalEvidence, recommendation, caseId, summary,
                openedAt, completedAt, null);
    }
    public InvestigationResult {
        findings = List.copyOf(findings);
        historicalEvidence = List.copyOf(historicalEvidence);
    }
}
