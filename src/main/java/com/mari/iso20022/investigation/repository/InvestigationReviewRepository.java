package com.mari.iso20022.investigation.repository;

import com.mari.iso20022.investigation.entity.InvestigationReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestigationReviewRepository extends JpaRepository<InvestigationReview, UUID> {
    List<InvestigationReview> findByInvestigationCase_IdOrderByReviewedAtAscCreatedAtAsc(UUID caseId);
}
