package com.mari.iso20022.commandcenter.service;

import com.mari.iso20022.commandcenter.dto.CommandCenterDtos.*;
import com.mari.iso20022.commandcenter.repository.CorrectiveActionRepository;
import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.entity.PaymentStatus;
import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.investigation.InvestigationService;
import com.mari.iso20022.investigation.repository.InvestigationCaseRepository;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final PaymentTransactionRepository paymentRepository;
    private final InvestigationCaseRepository caseRepository;
    private final InvestigationService investigationService;
    private final CorrectiveActionService actionService;
    private final CorrectiveActionRepository actionRepository;

    public DemoDataInitializer(PaymentTransactionRepository paymentRepository,
                               InvestigationCaseRepository caseRepository,
                               InvestigationService investigationService,
                               CorrectiveActionService actionService,
                               CorrectiveActionRepository actionRepository) {
        this.paymentRepository = paymentRepository;
        this.caseRepository = caseRepository;
        this.investigationService = investigationService;
        this.actionService = actionService;
        this.actionRepository = actionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            seedRegionalTransactions();
            runScenarioInvestigations();
            seedCorrectiveActions();
            log.info("Executive Command Center synthetic demonstration scenario and regional data initialized successfully.");
        } catch (Exception e) {
            log.warn("DemoDataInitializer completed with warning: {}", e.getMessage());
        }
    }

    private void seedRegionalTransactions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 1. Saudi Arabia (SARIE) - Corporate Payroll & Supplier Transfer
        createIfMissing("TXN-SA-1001", "MSG-SARIE-001", "SARIE", BigDecimal.valueOf(850000.00), "SAR",
                "Al Rajhi Industrial Holdings", "SA0380000000123456789012", "RJHI0001",
                "Riyadh Construction Materials", "SA4420000000987654321098", "NCBK0001",
                PaymentStatus.SUCCESS, now.minusDays(2));

        createIfMissing("TXN-SA-1002", "MSG-SARIE-002", "SARIE", BigDecimal.valueOf(1250000.00), "SAR",
                "Al Rajhi Industrial Holdings", "SA0380000000123456789012", "RJHI0001",
                "Jeddah Petrochemicals Corp", "SA8810000000555666777888", "ALBI0001",
                PaymentStatus.FAILED, now.minusHours(5));

        // 2. Bahrain (EFTS / Benefit) - Retail & Commercial Transfers
        createIfMissing("TXN-BH-2001", "MSG-EFTS-001", "EFTS", BigDecimal.valueOf(18500.00), "BHD",
                "Manama Trading Co BSC", "BH67BBK000000012345678", "BBKU0001",
                "Gulf Logistics Services", "BH29NBB000000098765432", "NBB00001",
                PaymentStatus.SUCCESS, now.minusDays(1));

        createIfMissing("TXN-BH-2002", "MSG-EFTS-002", "EFTS", BigDecimal.valueOf(9200.00), "BHD",
                "Manama Trading Co BSC", "BH67BBK000000012345678", "BBKU0001",
                "Bahrain Ports Authority", "BH11AUB000000044455566", "AUB00001",
                PaymentStatus.SUCCESS, now.minusHours(8));

        // 3. South Africa (SAMOS / PayShap) - Southern Africa Cross-border
        createIfMissing("TXN-ZA-3001", "MSG-SAMOS-001", "SAMOS", BigDecimal.valueOf(4200000.00), "ZAR",
                "Johannesburg Mining Consortium", "ZA12SBZA00000011223344", "SBZA0001",
                "Cape Town Export Logistics", "ZA45ABSA00000055667788", "ABSA0001",
                PaymentStatus.SUCCESS, now.minusDays(3));

        createIfMissing("TXN-ZA-3002", "MSG-SAMOS-002", "SAMOS", BigDecimal.valueOf(6800000.00), "ZAR",
                "Johannesburg Mining Consortium", "ZA12SBZA00000011223344", "SBZA0001",
                "Durban Maritime Freight", "ZA99FNB000000099887766", "FIRN0001",
                PaymentStatus.FAILED, now.minusHours(3));
    }

    private void createIfMissing(String txnId, String msgId, String settlementMethod, BigDecimal amount, String currency,
                                 String debtorName, String debtorIban, String debtorBic,
                                 String creditorName, String creditorIban, String creditorBic,
                                 PaymentStatus status, OffsetDateTime timestamp) {
        if (!paymentRepository.existsByTransactionId(txnId)) {
            Iso20022Payment payment = new Iso20022Payment(
                    msgId, timestamp, 1, settlementMethod,
                    "INSTR-" + txnId, "E2E-" + txnId, txnId, amount, currency,
                    LocalDate.now(), debtorName, debtorIban, debtorBic,
                    creditorName, creditorIban, creditorBic,
                    "Corporate Settlement Reference " + txnId
            );
            PaymentTransaction p = PaymentTransaction.create(payment, status);
            paymentRepository.save(p);
        }
    }

    private void runScenarioInvestigations() {
        for (String txnId : List.of("TXN100004", "TXN100006", "TXN-SA-1002", "TXN-ZA-3002")) {
            if (paymentRepository.existsByTransactionId(txnId) && caseRepository.findByTransactionIdOrderByCreatedAtDesc(txnId).isEmpty()) {
                try {
                    investigationService.investigate(txnId);
                } catch (Exception e) {
                    log.debug("Investigation on {} deferred: {}", txnId, e.getMessage());
                }
            }
        }
    }

    private void seedCorrectiveActions() {
        if (actionRepository.count() > 0) return;

        // Action 1: Completed action demonstrating measurable outcome improvement
        CorrectiveActionResponse act1 = actionService.createAction(new CreateActionRequest(
                null,
                "TXN100004",
                "Corporate Creditor Master Beneficiary Remediated",
                "Updated standing beneficiary IBAN for XYZ Supplies LLC following formal dual-control callback to Chief Financial Officer.",
                "BENEFICIARY_DATA_REMEDIATION",
                "Payments Operations",
                "Senior Settlement Officer",
                "Fahad Al-Maktoum",
                true,
                "Beneficiary Mismatch Rate",
                "14.2% Mismatch Failure Rate"
        ));

        actionService.processApproval(act1.id(), new ActionApprovalRequest(
                "APPROVED", "Mona Al-Suwaidi (VP Operations)",
                "CFO callback transcript verified against authorized corporate mandate. Beneficiary whitelist updated."
        ));

        actionService.addNote(act1.id(), new ActionNoteRequest(
                "Tariq Al-Mansoor", "L2 Payments Analyst",
                "Out-of-band callback performed via secure registered corporate phone token. Verified authentic invoice amendment."
        ));

        actionService.updateStatus(act1.id(), new ActionStatusUpdateRequest("COMPLETED"));

        actionService.updateOutcome(act1.id(), new ActionOutcomeUpdateRequest(
                "0.8% Mismatch Failure Rate",
                "MEASURED_IMPROVEMENT",
                "Consecutive payments for XYZ Supplies LLC settled without manual repair. Mismatch rate dropped from 14.2% to 0.8% over subsequent 14-day observation window."
        ));

        // Action 2: In-Progress Action on High Value Anomaly
        CorrectiveActionResponse act2 = actionService.createAction(new CreateActionRequest(
                null,
                "TXN100006",
                "Intraday Credit Liquidity Pre-Funding Release",
                "Payment value AED 150,000 exceeds corporate 3x historical baseline. Corporate relationship manager confirming seasonal inventory financing facility.",
                "CREDIT_LIMIT_REVIEW",
                "Corporate Relationship & Credit",
                "Relationship Manager",
                "Karim El-Sayed",
                true,
                "Intraday Settlement Buffer",
                "AED 150,000 Outlier Exposure"
        ));

        actionService.processApproval(act2.id(), new ActionApprovalRequest(
                "APPROVED", "Rashed Al-Nuaimi (Credit Risk Lead)",
                "Temporary intraday credit headroom granted against verified documentary LC #LC-2026-991."
        ));

        actionService.addNote(act2.id(), new ActionNoteRequest(
                "Karim El-Sayed", "Relationship Manager",
                "Treasury confirmed corporate liquidity line. Awaiting final clearing release batch."
        ));

        // Action 3: Proposed Action awaiting executive leadership decision
        actionService.createAction(new CreateActionRequest(
                null,
                "TXN-SA-1002",
                "Counterparty Clearing Rail Routing Diagnostic",
                "SARIE payment drop-out on Jeddah Petrochemicals Corp. Recommend verifying receiving bank participant status on Saudi Central Bank (SAMA) directory.",
                "CHANNEL_REPAIR",
                "Technology Operations",
                "Integration Specialist",
                "Zainab Al-Hassan",
                true,
                "SARIE Rejection Drop-Out Rate",
                "8.4% Gateway Exception Rate"
        ));
    }
}
