package com.mari.iso20022.commandcenter.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CommandCenterDtos {

    public record KpiSummary(
            long totalTransactions,
            BigDecimal totalAmountProcessed,
            String primaryCurrency,
            double straightThroughProcessingRate,
            double rejectionRate,
            double returnRate,
            long exceptionCount,
            long activeInvestigationsCount
    ) {}

    public record InvestigationSummary(
            long totalCases,
            long requiresReviewCount,
            long approvedCount,
            long rejectedCount,
            long escalatedCount,
            long closedCount
    ) {}

    public record RiskBreakdown(
            long criticalCount,
            long highCount,
            long mediumCount,
            long lowCount
    ) {}

    public record SlaMetrics(
            long breachedCount,
            long atRiskCount,
            long onTrackCount,
            double averageResolutionHours,
            double overallSlaComplianceRate
    ) {}

    public record AgeingBuckets(
            long under4Hours,
            long fourTo24Hours,
            long oneToThreeDays,
            long over3Days
    ) {}

    public record OperationalBottleneck(
            String ruleCode,
            String category,
            String description,
            long occurrenceCount,
            String severityLevel,
            String impactSummary
    ) {}

    public record ExecutiveOverview(
            KpiSummary kpiSummary,
            InvestigationSummary investigationSummary,
            RiskBreakdown riskBreakdown,
            SlaMetrics slaMetrics,
            AgeingBuckets ageingBuckets,
            List<OperationalBottleneck> bottlenecks,
            List<PriorityQueueItem> priorityAttentionQueue,
            long openCorrectiveActionsCount,
            String reportingPeriod,
            OffsetDateTime calculatedAt,
            String dataDisclaimer
    ) {}

    public record RailPerformance(
            String railCode,
            String railName,
            String marketCode,
            String messageType,
            long volume,
            BigDecimal value,
            double avgLatencyMs,
            double rejectionRate,
            String status
    ) {}

    public record MarketPerformance(
            String marketCode,
            String marketName,
            String country,
            String currency,
            List<String> activeRails,
            long totalVolume,
            BigDecimal totalValue,
            double stpRate,
            double rejectionRate,
            double returnRate,
            double avgProcessingDelayMinutes,
            long openInvestigations,
            double slaAdherencePercent,
            String operationalStatus,
            String primaryExceptionCategory,
            String demoDisclaimer
    ) {}

    public record RegionalIntelligence(
            List<MarketPerformance> markets,
            List<RailPerformance> rails,
            String observationWindow,
            OffsetDateTime generatedAt,
            String syntheticDataNotice
    ) {}

    public record ExceptionPattern(
            String patternId,
            String patternName,
            String category,
            String ruleCode,
            long affectedCount,
            BigDecimal affectedValueTotal,
            List<String> sampleTransactionIds,
            String detectionLogic,
            String confidenceLimitations,
            String recommendedNextStep,
            String operationalSeverity,
            String observedMarket
    ) {}

    public record PriorityScoreBreakdown(
            int operationalSeverityPoints,
            int slaUrgencyPoints,
            int volumeConcentrationPoints,
            int verificationConflictPoints,
            int totalScore
    ) {}

    public record PriorityQueueItem(
            String transactionId,
            UUID caseId,
            String market,
            String currency,
            BigDecimal amount,
            String debtorName,
            String creditorName,
            String severity,
            int riskScore,
            int priorityScore,
            PriorityScoreBreakdown scoreBreakdown,
            String slaStatus,
            double hoursInQueue,
            String recommendedAction,
            String assignedTeam,
            String workflowStatus,
            String verificationStatus
    ) {}

    public record ExecutiveDecisionBrief(
            String transactionId,
            UUID caseId,
            String headline,
            String observedSignal,
            BigDecimal amount,
            String currency,
            String debtor,
            String creditor,
            String market,
            List<String> supportingEvidence,
            String whatEvidenceEstablishes,
            String whatRemainsUncertain,
            String businessImplication,
            String recommendedNextStep,
            String accountableTeam,
            boolean humanApprovalRequired,
            String requiredApprovalRole,
            String verificationStatus,
            int verificationScore,
            String preservedRiskLevel,
            String followUpMeasure,
            boolean liveReasoningUsed,
            OffsetDateTime generatedAt
    ) {}

    public record CreateActionRequest(
            UUID caseId,
            String transactionId,
            String title,
            String description,
            String actionType,
            String assignedTeam,
            String assignedRole,
            String assignedTo,
            boolean humanApprovalRequired,
            String targetMetric,
            String baselineMetricValue
    ) {}

    public record ActionApprovalRequest(
            String decision, // APPROVED or REJECTED
            String reviewer,
            String notes
    ) {}

    public record ActionStatusUpdateRequest(
            String status // IN_PROGRESS, COMPLETED, CANCELLED
    ) {}

    public record ActionNoteRequest(
            String author,
            String role,
            String note
    ) {}

    public record ActionOutcomeUpdateRequest(
            String postActionMetricValue,
            String outcomeStatus, // MEASURED_IMPROVEMENT, NO_MATERIAL_CHANGE, INCONCLUSIVE
            String outcomeNotes
    ) {}

    public record ActionNoteResponse(
            UUID id,
            String author,
            String role,
            String note,
            OffsetDateTime createdAt
    ) {}

    public record CorrectiveActionResponse(
            UUID id,
            UUID caseId,
            String transactionId,
            String title,
            String description,
            String actionType,
            String status,
            String assignedTeam,
            String assignedRole,
            String assignedTo,
            boolean humanApprovalRequired,
            String approvalStatus,
            String approvedBy,
            OffsetDateTime approvedAt,
            String approvalNotes,
            String targetMetric,
            String baselineMetricValue,
            String postActionMetricValue,
            String outcomeStatus,
            String outcomeNotes,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime completedAt,
            List<ActionNoteResponse> notes
    ) {}
}
