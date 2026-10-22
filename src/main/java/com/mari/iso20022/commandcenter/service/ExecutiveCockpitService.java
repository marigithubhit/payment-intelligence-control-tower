package com.mari.iso20022.commandcenter.service;

import com.mari.iso20022.agent.AgentInvestigationResult;
import com.mari.iso20022.commandcenter.dto.CommandCenterDtos.*;
import com.mari.iso20022.commandcenter.repository.CorrectiveActionRepository;
import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.investigation.InvestigationService;
import com.mari.iso20022.investigation.dto.InvestigationResult;
import com.mari.iso20022.investigation.entity.InvestigationCase;
import com.mari.iso20022.investigation.entity.InvestigationEvidence;
import com.mari.iso20022.investigation.repository.InvestigationCaseRepository;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExecutiveCockpitService {

    private final PaymentTransactionRepository paymentRepository;
    private final InvestigationCaseRepository caseRepository;
    private final CorrectiveActionRepository actionRepository;
    private final InvestigationService investigationService;

    public ExecutiveCockpitService(PaymentTransactionRepository paymentRepository,
                                  InvestigationCaseRepository caseRepository,
                                  CorrectiveActionRepository actionRepository,
                                  InvestigationService investigationService) {
        this.paymentRepository = paymentRepository;
        this.caseRepository = caseRepository;
        this.actionRepository = actionRepository;
        this.investigationService = investigationService;
    }

    @Transactional(readOnly = true)
    public ExecutiveOverview getOverview() {
        List<PaymentTransaction> allPayments = paymentRepository.findAll();
        List<InvestigationCase> allCases = caseRepository.findAll();

        long totalTx = allPayments.size();
        BigDecimal totalAmount = allPayments.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successTx = allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS).count();
        long failedTx = allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.FAILED).count();
        long receivedTx = allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.RECEIVED).count();
        long returnedTx = allPayments.stream().filter(p -> "RETURNED".equalsIgnoreCase(p.getPaymentStatus().name())).count();

        double stpRate = totalTx > 0 ? (double) successTx / totalTx * 100.0 : 0.0;
        double rejectionRate = totalTx > 0 ? (double) failedTx / totalTx * 100.0 : 0.0;
        double returnRate = totalTx > 0 ? (double) returnedTx / totalTx * 100.0 : 0.0;
        long exceptionCount = totalTx - successTx;

        long openCases = allCases.stream().filter(c -> !isClosed(c.getStatus())).count();
        long requiresReview = allCases.stream().filter(c -> c.isHumanReviewRequired() || "REQUIRES_REVIEW".equalsIgnoreCase(c.getStatus())).count();
        long approvedCases = allCases.stream().filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus())).count();
        long rejectedCases = allCases.stream().filter(c -> "REJECTED".equalsIgnoreCase(c.getStatus())).count();
        long escalatedCases = allCases.stream().filter(c -> "ESCALATED".equalsIgnoreCase(c.getStatus())).count();
        long closedCases = allCases.stream().filter(c -> "CLOSED".equalsIgnoreCase(c.getStatus())).count();

        long criticalRisk = allCases.stream().filter(c -> "CRITICAL".equalsIgnoreCase(c.getRiskLevel())).count();
        long highRisk = allCases.stream().filter(c -> "HIGH".equalsIgnoreCase(c.getRiskLevel())).count();
        long mediumRisk = allCases.stream().filter(c -> "MEDIUM".equalsIgnoreCase(c.getRiskLevel())).count();
        long lowRisk = allCases.stream().filter(c -> "LOW".equalsIgnoreCase(c.getRiskLevel())).count();

        OffsetDateTime now = OffsetDateTime.now();
        long under4h = 0, fourTo24h = 0, oneTo3d = 0, over3d = 0;
        long breachedCount = 0, atRiskCount = 0, onTrackCount = 0;
        double totalHours = 0;
        int resolvedCount = 0;

        for (InvestigationCase c : allCases) {
            OffsetDateTime opened = c.getOpenedAt() != null ? c.getOpenedAt() : c.getCreatedAt();
            Duration duration = Duration.between(opened, c.getCompletedAt() != null ? c.getCompletedAt() : now);
            double hours = Math.max(0.1, duration.toMinutes() / 60.0);

            if (isClosed(c.getStatus())) {
                totalHours += hours;
                resolvedCount++;
            } else {
                if (hours < 4) under4h++;
                else if (hours < 24) fourTo24h++;
                else if (hours < 72) oneTo3d++;
                else over3d++;

                // SLA computation based on risk level
                double slaHours = switch (c.getRiskLevel().toUpperCase()) {
                    case "CRITICAL" -> 2.0;
                    case "HIGH" -> 4.0;
                    case "MEDIUM" -> 24.0;
                    default -> 48.0;
                };

                if (hours > slaHours) {
                    breachedCount++;
                } else if (hours > (slaHours * 0.8)) {
                    atRiskCount++;
                } else {
                    onTrackCount++;
                }
            }
        }

        double avgResolutionHours = resolvedCount > 0 ? totalHours / resolvedCount : 3.8;
        double complianceRate = allCases.isEmpty() ? 100.0 :
                Math.max(0.0, 100.0 - ((double) breachedCount / Math.max(1, openCases) * 100.0));

        // Bottlenecks from evidence
        Map<String, Long> ruleCounts = new HashMap<>();
        for (InvestigationCase c : allCases) {
            if (c.getEvidence() != null) {
                for (InvestigationEvidence e : c.getEvidence()) {
                    if (e.getRuleCode() != null) {
                        ruleCounts.put(e.getRuleCode(), ruleCounts.getOrDefault(e.getRuleCode(), 0L) + 1L);
                    }
                }
            }
        }

        List<OperationalBottleneck> bottlenecks = new ArrayList<>();
        if (ruleCounts.containsKey("BENEFICIARY_MISMATCH")) {
            bottlenecks.add(new OperationalBottleneck(
                    "BENEFICIARY_MISMATCH", "BENEFICIARY_DATA",
                    "Creditor IBAN deviates from established counterparty history in corporate transfers.",
                    ruleCounts.get("BENEFICIARY_MISMATCH"), "HIGH",
                    "Requires dual analyst verification to prevent fraudulent rerouting and misdirected settlement."
            ));
        }
        if (ruleCounts.containsKey("AMOUNT_ANOMALY")) {
            bottlenecks.add(new OperationalBottleneck(
                    "AMOUNT_ANOMALY", "OUTLIER_EXPOSURE",
                    "Payment value exceeds 3x historical corporate transfer baseline.",
                    ruleCounts.get("AMOUNT_ANOMALY"), "HIGH",
                    "Elevated liquidity exposure and concentration risk requiring treasury credit pre-approval."
            ));
        }
        if (ruleCounts.containsKey("REPEATED_PAYMENT_FAILURE")) {
            bottlenecks.add(new OperationalBottleneck(
                    "REPEATED_PAYMENT_FAILURE", "REPEATED_FAILURE",
                    "Multiple consecutive payment rejections for identical debtor-creditor pair within 90 days.",
                    ruleCounts.get("REPEATED_PAYMENT_FAILURE"), "MEDIUM",
                    "Channel configuration failure or inactive counterparty settlement account causing repair backlog."
            ));
        }
        if (ruleCounts.containsKey("IBAN_FORMAT")) {
            bottlenecks.add(new OperationalBottleneck(
                    "IBAN_FORMAT", "SYNTAX_VALIDATION",
                    "IBAN checksum or length validation failure on debtor/creditor account field.",
                    ruleCounts.get("IBAN_FORMAT"), "MEDIUM",
                    "Preventable STP drop-out at payment ingestion gateway requiring upstream validation."
            ));
        }
        if (bottlenecks.isEmpty()) {
            bottlenecks.add(new OperationalBottleneck(
                    "BENEFICIARY_MISMATCH", "BENEFICIARY_DATA",
                    "Creditor account differs from corporate counterparty baseline.",
                    Math.max(3L, failedTx), "HIGH",
                    "Manual repair queue latency impacting corporate SLA deadlines."
            ));
        }

        List<PriorityQueueItem> priorityQueue = buildPriorityQueue(allCases, allPayments).stream()
                .limit(10)
                .toList();

        long openActions = actionRepository.findByStatusOrderByCreatedAtDesc("PROPOSED").size()
                + actionRepository.findByStatusOrderByCreatedAtDesc("IN_PROGRESS").size();

        return new ExecutiveOverview(
                new KpiSummary(totalTx, totalAmount, "AED (Multi-Currency)", round(stpRate), round(rejectionRate),
                        round(returnRate), exceptionCount, openCases),
                new InvestigationSummary(allCases.size(), requiresReview, approvedCases, rejectedCases, escalatedCases, closedCases),
                new RiskBreakdown(criticalRisk, highRisk, mediumRisk, lowRisk),
                new SlaMetrics(breachedCount, atRiskCount, onTrackCount, round(avgResolutionHours), round(complianceRate)),
                new AgeingBuckets(under4h, fourTo24h, oneTo3d, over3d),
                bottlenecks,
                priorityQueue,
                openActions,
                "Rolling 90-Day Operational Window",
                now,
                "DEMO DATA: Synthetic regional transactions across UAE, Saudi Arabia, Bahrain, and South Africa for executive prototype evaluation."
        );
    }

    @Transactional(readOnly = true)
    public RegionalIntelligence getRegionalIntelligence() {
        List<PaymentTransaction> allPayments = paymentRepository.findAll();
        List<InvestigationCase> allCases = caseRepository.findAll();

        Map<String, List<PaymentTransaction>> byMarket = allPayments.stream()
                .collect(Collectors.groupingBy(this::detectMarketCode));

        List<MarketPerformance> markets = new ArrayList<>();

        // 1. UAE
        markets.add(buildMarketPerformance("AE", "United Arab Emirates", "UAE", "AED",
                List.of("UAE-FTS (RTGS)", "UAE-IPP / AANI", "UAE-ECC"),
                byMarket.getOrDefault("AE", List.of()), allCases,
                "STP rate affected by cross-border beneficiary repairs on GCC corridors."));

        // 2. Saudi Arabia
        markets.add(buildMarketPerformance("SA", "Kingdom of Saudi Arabia", "Saudi Arabia", "SAR",
                List.of("SARIE (RTGS)", "IPS / sari", "B2B Gateway"),
                byMarket.getOrDefault("SA", List.of()), allCases,
                "High STP rate with localized IBAN validation on corporate payroll rails."));

        // 3. Bahrain
        markets.add(buildMarketPerformance("BH", "Kingdom of Bahrain", "Bahrain", "BHD",
                List.of("EFTS (Fawri+ / Fawri)", "Benefit", "RTGS"),
                byMarket.getOrDefault("BH", List.of()), allCases,
                "Fawri+ instant retail transfers operating within sub-second thresholds."));

        // 4. South Africa
        markets.add(buildMarketPerformance("ZA", "Republic of South Africa", "South Africa", "ZAR",
                List.of("SAMOS (RTGS)", "PayShap (BankservAfrica)", "SADC-RTGS"),
                byMarket.getOrDefault("ZA", List.of()), allCases,
                "Cross-border SADC-RTGS liquidity settlement delays during end-of-day clearing."));

        // Rail breakdown
        List<RailPerformance> rails = List.of(
                new RailPerformance("UAE-FTS", "UAE Fund Transfer System (RTGS)", "AE", "pacs.008.001.08",
                        Math.max(120, byMarket.getOrDefault("AE", List.of()).size() * 12),
                        BigDecimal.valueOf(14500000.00), 45.2, 1.8, "OPERATIONAL"),
                new RailPerformance("AANI", "UAE Instant Payments Platform (IPP)", "AE", "pacs.008.001.08",
                        Math.max(450, byMarket.getOrDefault("AE", List.of()).size() * 35),
                        BigDecimal.valueOf(3200000.00), 1.2, 0.6, "OPERATIONAL"),
                new RailPerformance("SARIE", "Saudi Arabian Real-time Interbank (RTGS)", "SA", "pacs.008.001.08",
                        Math.max(210, byMarket.getOrDefault("SA", List.of()).size() * 18),
                        BigDecimal.valueOf(28000000.00), 52.0, 1.2, "OPERATIONAL"),
                new RailPerformance("IPS-KSA", "Saudi Instant Payment System", "SA", "pacs.008.001.08",
                        Math.max(620, byMarket.getOrDefault("SA", List.of()).size() * 45),
                        BigDecimal.valueOf(5100000.00), 1.5, 0.4, "OPERATIONAL"),
                new RailPerformance("EFTS-BH", "Bahrain Electronic Fund Transfer", "BH", "pacs.008.001.08",
                        Math.max(95, byMarket.getOrDefault("BH", List.of()).size() * 10),
                        BigDecimal.valueOf(1800000.00), 2.1, 0.9, "OPERATIONAL"),
                new RailPerformance("PayShap", "BankservAfrica PayShap (Low Value Instant)", "ZA", "pacs.008.001.08",
                        Math.max(340, byMarket.getOrDefault("ZA", List.of()).size() * 25),
                        BigDecimal.valueOf(9800000.00), 1.8, 1.4, "OPERATIONAL"),
                new RailPerformance("SAMOS", "South African Multiple Option Settlement", "ZA", "pacs.008.001.08",
                        Math.max(80, byMarket.getOrDefault("ZA", List.of()).size() * 8),
                        BigDecimal.valueOf(42000000.00), 65.4, 2.8, "DEGRADED_LATENCY")
        );

        return new RegionalIntelligence(
                markets,
                rails,
                "Configured Demo MEA Corridors (90-day observation window)",
                OffsetDateTime.now(),
                "DEMO DATA: Regional payment rails, volumes and latencies are generated for executive demonstration purposes."
        );
    }

    @Transactional(readOnly = true)
    public List<ExceptionPattern> getExceptionPatterns() {
        List<PaymentTransaction> payments = paymentRepository.findAll();
        List<InvestigationCase> cases = caseRepository.findAll();

        List<ExceptionPattern> patterns = new ArrayList<>();

        // Pattern 1: Recurring Beneficiary Mismatch on UAE Corridors
        List<String> mismatchTxns = cases.stream()
                .filter(c -> c.getEvidence() != null && c.getEvidence().stream().anyMatch(e -> "BENEFICIARY_MISMATCH".equals(e.getRuleCode())))
                .map(InvestigationCase::getTransactionId)
                .distinct()
                .toList();

        patterns.add(new ExceptionPattern(
                "PAT-MEA-001",
                "Corporate Creditor IBAN Account Substitution",
                "BENEFICIARY_INTEGRITY",
                "BENEFICIARY_MISMATCH",
                Math.max(mismatchTxns.size(), 3),
                BigDecimal.valueOf(485000.00),
                mismatchTxns.isEmpty() ? List.of("TXN100004", "TXN100015") : mismatchTxns,
                "Deterministic rule comparing submitted creditor IBAN against 90-day historical settled transactions for identical debtor entity.",
                "Limited to known counterparty history; unestablished first-time counterparties cannot produce a mismatch signal.",
                "Route to Corporate Dual-Authorization Queue; require corporate treasurer callback verification before clearing hold.",
                "HIGH",
                "UAE / GCC Cross-Border"
        ));

        // Pattern 2: Outlier Velocity / Large Value Deviation
        List<String> anomalyTxns = cases.stream()
                .filter(c -> c.getEvidence() != null && c.getEvidence().stream().anyMatch(e -> "AMOUNT_ANOMALY".equals(e.getRuleCode())))
                .map(InvestigationCase::getTransactionId)
                .distinct()
                .toList();

        patterns.add(new ExceptionPattern(
                "PAT-MEA-002",
                "High-Value Outlier Exceeding 3x Historical Average",
                "EXPOSURE_RISK",
                "AMOUNT_ANOMALY",
                Math.max(anomalyTxns.size(), 2),
                BigDecimal.valueOf(1500000.00),
                anomalyTxns.isEmpty() ? List.of("TXN100006") : anomalyTxns,
                "Calculated baseline of settled transactions in identical currency; flags payments exceeding 3.0x multiplier threshold.",
                "Requires minimum of 3 settled payments to establish baseline; new accounts default to unflagged.",
                "Confirm intraday corporate liquidity facility and senior treasury credit committee sanction before settlement release.",
                "HIGH",
                "Saudi Arabia (SARIE) / UAE (FTS)"
        ));

        // Pattern 3: Recurrent Payment Rejections / Closed Account (AC04)
        patterns.add(new ExceptionPattern(
                "PAT-MEA-003",
                "Repeated Account Invalidation Drop-Outs (ISO AC04)",
                "REPAIR_BACKLOG",
                "REPEATED_PAYMENT_FAILURE",
                4,
                BigDecimal.valueOf(75000.00),
                List.of("TXN100008", "TXN100009", "TXN100010"),
                "3 or more consecutive settlement rejections for identical debtor-creditor pair within 90 days.",
                "Does not differentiate between temporary beneficiary account blocks and permanent closure.",
                "Initiate automated customer outreach to update corporate counterparty master standing instructions.",
                "MEDIUM",
                "Bahrain (EFTS) & UAE"
        ));

        // Pattern 4: Clearing Latency Spikes on Southern Africa Corridor
        patterns.add(new ExceptionPattern(
                "PAT-MEA-004",
                "End-of-Day SADC-RTGS Multilateral Settlement Delays",
                "SYSTEMIC_LATENCY",
                "CLEARING_BOTTLENECK",
                2,
                BigDecimal.valueOf(2340000.00),
                List.of("TXN-ZA-2001", "TXN-ZA-2003"),
                "Settlement acknowledgment delay exceeding 45-minute clearing window SLA during cross-border cut-off hours.",
                "External clearing participant response latency; internal gateway processing normal.",
                "Trigger automated liquidity pre-funding alerts and notify regional corporate cash management desk.",
                "MEDIUM",
                "South Africa (SAMOS / SADC)"
        ));

        return patterns;
    }

    @Transactional(readOnly = true)
    public List<PriorityQueueItem> getPrioritizedQueue() {
        List<InvestigationCase> allCases = caseRepository.findAll();
        List<PaymentTransaction> allPayments = paymentRepository.findAll();
        return buildPriorityQueue(allCases, allPayments);
    }

    @Transactional
    public ExecutiveDecisionBrief getDecisionBrief(String transactionId) {
        PaymentTransaction txn = paymentRepository.findByTransactionId(transactionId)
                .orElse(null);

        InvestigationResult investigation;
        try {
            investigation = investigationService.investigate(transactionId);
        } catch (Exception e) {
            // Return fallback brief if investigation throws
            return buildFallbackBrief(transactionId, txn);
        }

        UUID caseId = investigation.caseId() != null ? UUID.fromString(investigation.caseId()) : null;
        AgentInvestigationResult agents = investigation.agentAssessment();

        String market = txn != null ? detectMarketName(detectMarketCode(txn)) : "UAE / GCC";
        String currency = txn != null ? txn.getCurrency() : "AED";
        BigDecimal amount = txn != null ? txn.getAmount() : BigDecimal.valueOf(25000);
        String debtor = txn != null ? txn.getDebtorName() : "Corporate Debtor";
        String creditor = txn != null ? txn.getCreditorName() : "Creditor Entity";

        List<String> evidenceLines = new ArrayList<>();
        if (investigation.findings() != null) {
            for (var f : investigation.findings()) {
                if (!f.passed()) {
                    evidenceLines.add("[" + f.ruleCode() + "] " + f.message() + (f.evidence() != null ? " (" + f.evidence() + ")" : ""));
                }
            }
        }
        if (evidenceLines.isEmpty()) {
            evidenceLines.add("All deterministic rules satisfied; clean payment history verified.");
        }

        String headline;
        String observedSignal;
        String whatEvidenceEstablishes;
        String whatRemainsUncertain;
        String businessImplication;
        String recommendedNextStep;
        String accountableTeam;
        String approvalRole;
        boolean approvalRequired = "HIGH".equalsIgnoreCase(investigation.riskLevel().name())
                || "CRITICAL".equalsIgnoreCase(investigation.riskLevel().name())
                || investigation.status().toString().contains("REVIEW");

        String verificationStatus = "UNVERIFIED";
        int verificationScore = 0;
        String preservedRisk = investigation.riskLevel().name();
        boolean liveReasoningUsed = false;

        if (agents != null && agents.verificationAgent() != null) {
            verificationStatus = agents.verificationAgent().verificationStatus().name();
            verificationScore = agents.verificationAgent().verificationScore();
            preservedRisk = agents.verificationAgent().riskLevelPreserved().name();
        }

        if (agents != null && agents.reasoningAgent() != null && agents.reasoningAgent().reasoningStatus() != null) {
            liveReasoningUsed = "COMPLETED".equalsIgnoreCase(agents.reasoningAgent().reasoningStatus().name())
                    || "AVAILABLE".equalsIgnoreCase(agents.reasoningAgent().reasoningStatus().name());
        }

        if ("CRITICAL".equalsIgnoreCase(preservedRisk) || "HIGH".equalsIgnoreCase(preservedRisk)) {
            headline = "Material Exception: Counterparty Divergence on " + currency + " " + amount.toPlainString() + " Payment";
            observedSignal = "Beneficiary account divergence identified on payment " + transactionId + " originating from " + debtor + ".";
            whatEvidenceEstablishes = "Creditor account differs from established prior successful settlement records in the past 90 days. Beneficiary format passed IBAN syntax check.";
            whatRemainsUncertain = "Whether debtor updated counterparty details via out-of-band invoice or whether payment instruction represents an unauthorized account substitution.";
            businessImplication = "Direct financial exposure of " + currency + " " + amount.toPlainString() + "; regulatory reporting liability if misdirected; relationship friction if delayed past SLA cut-off.";
            recommendedNextStep = "Place payment in HOLD status; dispatch secure corporate treasurer callback token; review historical invoices.";
            accountableTeam = "Payments Operations & Fraud Risk Review Desk";
            approvalRole = "Senior Transaction Banking Operations Officer";
        } else if ("MEDIUM".equalsIgnoreCase(preservedRisk)) {
            headline = "Operational Review: Rule Threshold Triggered on " + transactionId;
            observedSignal = "Payment triggered secondary investigation threshold (recent failure pattern or currency validation).";
            whatEvidenceEstablishes = "Counterparty history shows repeated failed attempts or unconfigured currency attributes.";
            whatRemainsUncertain = "Root cause of prior settlement drop-outs at receiving clearing participant.";
            businessImplication = "Manual repair overhead; potential straight-through processing SLA delay.";
            recommendedNextStep = "Review receiving bank BIC routing table and confirm counterparty account active status.";
            accountableTeam = "Payment Repair & Exceptions Operations";
            approvalRole = "Payment Operations Supervisor";
        } else {
            headline = "Straight-Through Clearance: " + transactionId + " Verified Compliant";
            observedSignal = "Clean ISO 20022 message parameters matching established counterparty baseline.";
            whatEvidenceEstablishes = "All 8 deterministic validation rules passed; historical counterparty records match.";
            whatRemainsUncertain = "None; standard operational clearance.";
            businessImplication = "Zero exception exposure; eligible for automated instant clearing release.";
            recommendedNextStep = "Release payment to clearing network under standard STP pipeline.";
            accountableTeam = "Automated STP Clearing Engine";
            approvalRole = "Automated System Clearance (No Human Approval Required)";
        }

        return new ExecutiveDecisionBrief(
                transactionId,
                caseId,
                headline,
                observedSignal,
                amount,
                currency,
                debtor,
                creditor,
                market,
                evidenceLines,
                whatEvidenceEstablishes,
                whatRemainsUncertain,
                businessImplication,
                recommendedNextStep,
                accountableTeam,
                approvalRequired,
                approvalRole,
                verificationStatus,
                verificationScore,
                preservedRisk,
                "Monitor receiving clearing acknowledgment (pacs.002) within 15 minutes of release.",
                liveReasoningUsed,
                OffsetDateTime.now()
        );
    }

    private ExecutiveDecisionBrief buildFallbackBrief(String transactionId, PaymentTransaction txn) {
        return new ExecutiveDecisionBrief(
                transactionId,
                null,
                "Payment Investigation Brief: " + transactionId,
                "Transaction record loaded from database.",
                txn != null ? txn.getAmount() : BigDecimal.ZERO,
                txn != null ? txn.getCurrency() : "AED",
                txn != null ? txn.getDebtorName() : "Unknown",
                txn != null ? txn.getCreditorName() : "Unknown",
                "UAE / Middle East",
                List.of("Historical analysis pending investigation execution."),
                "Payment persisted in database.",
                "Agent analysis not yet completed.",
                "Review required by transaction operations team.",
                "Initiate formal case investigation.",
                "Payment Operations",
                true,
                "Operations Supervisor",
                "PENDING",
                0,
                "MEDIUM",
                "Verify payment status after investigation run.",
                false,
                OffsetDateTime.now()
        );
    }

    private List<PriorityQueueItem> buildPriorityQueue(List<InvestigationCase> cases, List<PaymentTransaction> payments) {
        Map<String, PaymentTransaction> txnMap = payments.stream()
                .collect(Collectors.toMap(PaymentTransaction::getTransactionId, p -> p, (a, b) -> a));

        List<PriorityQueueItem> items = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (InvestigationCase c : cases) {
            PaymentTransaction p = txnMap.get(c.getTransactionId());
            double hoursInQueue = c.getOpenedAt() != null ?
                    Duration.between(c.getOpenedAt(), now).toMinutes() / 60.0 : 1.5;

            // Score components
            int severityPoints = switch (c.getRiskLevel().toUpperCase()) {
                case "CRITICAL" -> 40;
                case "HIGH" -> 25;
                case "MEDIUM" -> 10;
                default -> 5;
            };

            double slaThreshold = switch (c.getRiskLevel().toUpperCase()) {
                case "CRITICAL" -> 2.0;
                case "HIGH" -> 4.0;
                case "MEDIUM" -> 24.0;
                default -> 48.0;
            };

            int slaUrgencyPoints;
            String slaStatus;
            if (hoursInQueue > slaThreshold) {
                slaUrgencyPoints = 30;
                slaStatus = "BREACHED";
            } else if (hoursInQueue > (slaThreshold * 0.8)) {
                slaUrgencyPoints = 20;
                slaStatus = "AT_RISK";
            } else {
                slaUrgencyPoints = 5;
                slaStatus = "ON_TRACK";
            }

            int concentrationPoints = 0;
            if (p != null && (p.getDebtorName().contains("ABC") || p.getCreditorName().contains("XYZ"))) {
                concentrationPoints = 15;
            }

            int verificationConflictPoints = 0;
            String verificationStatus = "VERIFIED";
            if (c.getSummary() != null && c.getSummary().contains("multiple findings")) {
                verificationConflictPoints = 15;
                verificationStatus = "FLAGGED_FINDINGS";
            }

            int totalScore = Math.min(100, severityPoints + slaUrgencyPoints + concentrationPoints + verificationConflictPoints);

            PriorityScoreBreakdown breakdown = new PriorityScoreBreakdown(
                    severityPoints, slaUrgencyPoints, concentrationPoints, verificationConflictPoints, totalScore
            );

            String market = p != null ? detectMarketCode(p) : "AE";
            String team = "CRITICAL".equalsIgnoreCase(c.getRiskLevel()) || "HIGH".equalsIgnoreCase(c.getRiskLevel()) ?
                    "Risk & Compliance / Senior Ops" : "Payments Repair Desk";

            items.add(new PriorityQueueItem(
                    c.getTransactionId(),
                    c.getId(),
                    market,
                    p != null ? p.getCurrency() : "AED",
                    p != null ? p.getAmount() : BigDecimal.valueOf(25000),
                    p != null ? p.getDebtorName() : "Debtor LLC",
                    p != null ? p.getCreditorName() : "Creditor LLC",
                    c.getRiskLevel(),
                    severityPoints * 2,
                    totalScore,
                    breakdown,
                    slaStatus,
                    round(hoursInQueue),
                    c.getRecommendation(),
                    team,
                    c.getStatus(),
                    verificationStatus
            ));
        }

        // Sort descending by priority score
        items.sort(Comparator.comparingInt(PriorityQueueItem::priorityScore).reversed());
        return items;
    }

    private MarketPerformance buildMarketPerformance(String code, String name, String country, String currency,
                                                    List<String> rails, List<PaymentTransaction> txns,
                                                    List<InvestigationCase> allCases, String summary) {
        long vol = Math.max(txns.size(), 8);
        BigDecimal val = txns.stream().map(PaymentTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (val.compareTo(BigDecimal.ZERO) == 0) {
            val = BigDecimal.valueOf(450000.00);
        }

        long success = txns.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS).count();
        long failed = txns.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.FAILED).count();
        long returned = txns.stream().filter(p -> "RETURNED".equalsIgnoreCase(p.getPaymentStatus().name())).count();

        double stp = vol > 0 ? (double) (success > 0 ? success : vol - 2) / vol * 100.0 : 92.4;
        double rej = vol > 0 ? (double) (failed > 0 ? failed : 1) / vol * 100.0 : 4.1;
        double ret = vol > 0 ? (double) (returned > 0 ? returned : 0) / vol * 100.0 : 1.2;

        double delay = switch (code) {
            case "AE" -> 1.8;
            case "SA" -> 2.4;
            case "BH" -> 0.9;
            case "ZA" -> 4.5;
            default -> 2.0;
        };

        double sla = switch (code) {
            case "AE" -> 96.8;
            case "SA" -> 98.2;
            case "BH" -> 99.4;
            case "ZA" -> 91.5;
            default -> 95.0;
        };

        long openInv = allCases.stream()
                .filter(c -> !isClosed(c.getStatus()))
                .filter(c -> code.equalsIgnoreCase(detectMarketFromIban(c.getTransactionId())))
                .count();

        return new MarketPerformance(
                code, name, country, currency, rails, vol, val, round(stp), round(rej), round(ret),
                delay, Math.max(openInv, 2), sla, "ACTIVE",
                code.equals("AE") ? "BENEFICIARY_MISMATCH" : code.equals("SA") ? "AMOUNT_ANOMALY" : "ROUTING_DELAY",
                "DEMO DATA: Synthetic regional banking activity for " + name + " (" + currency + ")."
        );
    }

    private String detectMarketCode(PaymentTransaction p) {
        if (p.getDebtorIban() != null && p.getDebtorIban().length() >= 2) {
            String prefix = p.getDebtorIban().substring(0, 2).toUpperCase();
            if (Set.of("AE", "SA", "BH", "ZA").contains(prefix)) return prefix;
        }
        if (p.getCreditorIban() != null && p.getCreditorIban().length() >= 2) {
            String prefix = p.getCreditorIban().substring(0, 2).toUpperCase();
            if (Set.of("AE", "SA", "BH", "ZA").contains(prefix)) return prefix;
        }
        if ("SAR".equalsIgnoreCase(p.getCurrency())) return "SA";
        if ("BHD".equalsIgnoreCase(p.getCurrency())) return "BH";
        if ("ZAR".equalsIgnoreCase(p.getCurrency())) return "ZA";
        return "AE";
    }

    private String detectMarketFromIban(String identifier) {
        if (identifier != null && identifier.startsWith("TXN-SA")) return "SA";
        if (identifier != null && identifier.startsWith("TXN-BH")) return "BH";
        if (identifier != null && identifier.startsWith("TXN-ZA")) return "ZA";
        return "AE";
    }

    private String detectMarketName(String code) {
        return switch (code) {
            case "SA" -> "Saudi Arabia (SARIE)";
            case "BH" -> "Bahrain (EFTS)";
            case "ZA" -> "South Africa (SAMOS)";
            default -> "United Arab Emirates (UAE-FTS)";
        };
    }

    private boolean isClosed(String status) {
        return "CLOSED".equalsIgnoreCase(status)
                || "APPROVED".equalsIgnoreCase(status)
                || "REJECTED".equalsIgnoreCase(status);
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
