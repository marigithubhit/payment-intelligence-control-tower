package com.mari.iso20022.investigation;

import com.mari.iso20022.exception.InvestigationCaseNotFoundException;
import com.mari.iso20022.exception.InvalidReviewTransitionException;
import com.mari.iso20022.investigation.dto.InvestigationReviewRequest;
import com.mari.iso20022.investigation.dto.InvestigationReviewResponse;
import com.mari.iso20022.investigation.dto.ReviewDecision;
import com.mari.iso20022.investigation.entity.InvestigationCase;
import com.mari.iso20022.investigation.entity.InvestigationReview;
import com.mari.iso20022.investigation.repository.InvestigationCaseRepository;
import com.mari.iso20022.investigation.repository.InvestigationReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HumanReviewService {
    private final InvestigationCaseRepository caseRepository;
    private final InvestigationReviewRepository reviewRepository;

    public HumanReviewService(InvestigationCaseRepository caseRepository,
                              InvestigationReviewRepository reviewRepository) {
        this.caseRepository = caseRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public InvestigationReviewResponse submit(UUID caseId, InvestigationReviewRequest request) {
        InvestigationCase investigationCase = caseRepository.findByIdForUpdate(caseId)
                .orElseThrow(() -> new InvestigationCaseNotFoundException(caseId));
        String previousStatus = investigationCase.getStatus();
        if (!isEligible(investigationCase)) {
            throw new InvalidReviewTransitionException("This investigation is not eligible for human review");
        }
        if ("CLOSED".equals(previousStatus) || "APPROVED".equals(previousStatus)
                || "REJECTED".equals(previousStatus)) {
            throw new InvalidReviewTransitionException("Investigation status " + previousStatus
                    + " does not accept another review decision");
        }
        if ("ESCALATED".equals(previousStatus) && request.decision() == ReviewDecision.ESCALATE) {
            throw new InvalidReviewTransitionException("An escalated investigation must be reviewed before another escalation");
        }
        if (!List.of("CLEAR", "REQUIRES_REVIEW", "OPEN", "UNDER_REVIEW", "ESCALATED")
                .contains(previousStatus)) {
            throw new InvalidReviewTransitionException("Investigation status " + previousStatus
                    + " cannot transition to a review decision");
        }

        String newStatus = switch (request.decision()) {
            case APPROVE -> "APPROVED";
            case REJECT -> "REJECTED";
            case ESCALATE -> "ESCALATED";
        };
        investigationCase.updateWorkflowStatus(newStatus);
        caseRepository.save(investigationCase);
        InvestigationReview review = InvestigationReview.record(investigationCase, request.decision(),
                request.reason().trim(), request.reviewer().trim(), previousStatus, newStatus);
        return InvestigationReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    @Transactional(readOnly = true)
    public List<InvestigationReviewResponse> history(UUID caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new InvestigationCaseNotFoundException(caseId);
        }
        return reviewRepository.findByInvestigationCase_IdOrderByReviewedAtAscCreatedAtAsc(caseId)
                .stream().map(InvestigationReviewResponse::from).toList();
    }

    private boolean isEligible(InvestigationCase investigationCase) {
        return investigationCase.isHumanReviewRequired()
                || "REQUIRES_REVIEW".equals(investigationCase.getStatus())
                || "OPEN".equals(investigationCase.getStatus())
                || "UNDER_REVIEW".equals(investigationCase.getStatus())
                || "ESCALATED".equals(investigationCase.getStatus())
                || "HIGH".equals(investigationCase.getRiskLevel())
                || "CRITICAL".equals(investigationCase.getRiskLevel());
    }
}
