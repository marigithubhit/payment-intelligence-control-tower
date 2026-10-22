package com.mari.iso20022.investigation;

import com.mari.iso20022.exception.InvestigationCaseNotFoundException;
import com.mari.iso20022.investigation.dto.CollectedEvidence;
import com.mari.iso20022.investigation.dto.InvestigationCaseResponse;
import com.mari.iso20022.investigation.dto.InvestigationEvidenceResponse;
import com.mari.iso20022.investigation.dto.InvestigationResult;
import com.mari.iso20022.investigation.entity.InvestigationCase;
import com.mari.iso20022.investigation.entity.InvestigationEvidence;
import com.mari.iso20022.investigation.repository.InvestigationCaseRepository;
import com.mari.iso20022.investigation.repository.InvestigationEvidenceRepository;
import com.mari.iso20022.validation.ValidationFinding;
import com.mari.iso20022.validation.ValidationSeverity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InvestigationPersistenceService {
    private final InvestigationCaseRepository caseRepository;
    private final InvestigationEvidenceRepository evidenceRepository;

    public InvestigationPersistenceService(InvestigationCaseRepository caseRepository,
                                           InvestigationEvidenceRepository evidenceRepository) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional
    public InvestigationCase persist(InvestigationResult result) {
        return persist(result, List.of());
    }

    @Transactional
    public InvestigationCase persist(InvestigationResult result, List<CollectedEvidence> collectedEvidence) {
        InvestigationCase investigationCase = InvestigationCase.from(result, createSummary(result));
        for (ValidationFinding finding : result.findings()) {
            investigationCase.addEvidence(InvestigationEvidence.validationFinding(
                    finding.category().name(), finding.ruleCode(), finding.severity().name(),
                    finding.message() + " Evidence: " + finding.evidence(), "Phase4ValidationEngine"));
        }
        collectedEvidence.stream().map(InvestigationEvidence::from).forEach(investigationCase::addEvidence);
        return caseRepository.saveAndFlush(investigationCase);
    }

    @Transactional(readOnly = true)
    public InvestigationCaseResponse getCase(UUID caseId) {
        InvestigationCase investigationCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new InvestigationCaseNotFoundException(caseId));
        List<InvestigationEvidenceResponse> evidence = evidenceRepository
                .findByInvestigationCase_IdOrderByCreatedAtAsc(caseId).stream()
                .map(InvestigationEvidenceResponse::from).toList();
        return InvestigationCaseResponse.from(investigationCase, evidence);
    }

    @Transactional
    public void markHumanReviewRequired(UUID caseId) {
        InvestigationCase investigationCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new InvestigationCaseNotFoundException(caseId));
        investigationCase.markHumanReviewRequired();
    }

    private String createSummary(InvestigationResult result) {
        if (result.status().name().equals("CLEAR")) {
            return "Payment transaction passed all configured validation rules. No investigation findings were identified.";
        }
        if (result.findings().size() > 1) {
            return "Payment transaction requires review due to multiple validation findings.";
        }
        if (result.findings().stream().anyMatch(finding -> finding.severity() == ValidationSeverity.HIGH
                || finding.severity() == ValidationSeverity.CRITICAL)) {
            return "Payment transaction requires manual review due to one or more high-severity investigation findings.";
        }
        return "Payment transaction requires review due to one or more validation findings.";
    }
}
