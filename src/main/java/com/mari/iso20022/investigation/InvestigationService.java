package com.mari.iso20022.investigation;

import com.mari.iso20022.entity.PaymentTransaction;
import com.mari.iso20022.agent.AgentInvestigationResult;
import com.mari.iso20022.agent.InvestigationAgentOrchestrator;
import com.mari.iso20022.exception.TransactionNotFoundException;
import com.mari.iso20022.investigation.dto.InvestigationResult;
import com.mari.iso20022.investigation.dto.InvestigationStatus;
import com.mari.iso20022.investigation.dto.RiskLevel;
import com.mari.iso20022.investigation.entity.InvestigationCase;
import com.mari.iso20022.repository.PaymentTransactionRepository;
import com.mari.iso20022.validation.PaymentValidationResult;
import com.mari.iso20022.validation.PaymentValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestigationService {
    private final PaymentTransactionRepository repository;
    private final PaymentValidationService validationService;
    private final RiskAssessmentService riskAssessmentService;
    private final InvestigationPersistenceService persistenceService;
    private final HistoricalAnalysisService historicalAnalysisService;
    private final EvidenceCollectionService evidenceCollectionService;
    private final InvestigationAgentOrchestrator agentOrchestrator;

    public InvestigationService(PaymentTransactionRepository repository,
                                PaymentValidationService validationService,
                                RiskAssessmentService riskAssessmentService,
                                InvestigationPersistenceService persistenceService,
                                HistoricalAnalysisService historicalAnalysisService,
                                EvidenceCollectionService evidenceCollectionService,
                                InvestigationAgentOrchestrator agentOrchestrator) {
        this.repository = repository;
        this.validationService = validationService;
        this.riskAssessmentService = riskAssessmentService;
        this.persistenceService = persistenceService;
        this.historicalAnalysisService = historicalAnalysisService;
        this.evidenceCollectionService = evidenceCollectionService;
        this.agentOrchestrator = agentOrchestrator;
    }

    @Transactional
    public InvestigationResult investigate(String transactionId) {
        PaymentTransaction transaction = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        PaymentValidationResult validation = validationService.validate(transaction);
        var historicalAnalysis = historicalAnalysisService.analyze(transaction);
        var collectedEvidence = evidenceCollectionService.collect(transaction, historicalAnalysis, validation.findings());
        InvestigationStatus status = riskAssessmentService.status(validation.findings());
        RiskLevel riskLevel = riskAssessmentService.assess(validation.findings());
        InvestigationResult calculated = new InvestigationResult(transactionId, status, riskLevel,
                validation.findings(), validation.historicalEvidence(),
                riskAssessmentService.recommendation(status, riskLevel), null, null, null, null);
        InvestigationCase persisted = persistenceService.persist(calculated, collectedEvidence);
        var caseResponse = persistenceService.getCase(persisted.getId());
        AgentInvestigationResult agentAssessment = agentOrchestrator.assess(transaction.toPayment(), validation.findings(), historicalAnalysis,
                caseResponse.evidence());
        boolean humanReviewRequired = status == InvestigationStatus.REQUIRES_REVIEW
                || riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL
                || (agentAssessment.evidenceAgent() != null
                    && (agentAssessment.evidenceAgent().conflictingEvidence()
                        || agentAssessment.evidenceAgent().evidenceCompleteness()
                            == com.mari.iso20022.agent.evidence.EvidenceCompleteness.INSUFFICIENT))
                || (agentAssessment.verificationAgent() != null
                    && (agentAssessment.verificationAgent().requiresHumanReview()
                        || agentAssessment.verificationAgent().verificationStatus()
                            == com.mari.iso20022.agent.verification.VerificationStatus.REJECTED));
        if (humanReviewRequired) {
            persistenceService.markHumanReviewRequired(persisted.getId());
        }
        return new InvestigationResult(transactionId, status, riskLevel, validation.findings(),
                validation.historicalEvidence(), calculated.recommendation(), persisted.getId().toString(),
                persisted.getSummary(), persisted.getOpenedAt(), persisted.getCompletedAt(), agentAssessment);
    }
}
