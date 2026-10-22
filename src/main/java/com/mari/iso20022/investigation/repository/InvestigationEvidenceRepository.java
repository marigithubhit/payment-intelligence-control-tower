package com.mari.iso20022.investigation.repository;

import com.mari.iso20022.investigation.entity.InvestigationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestigationEvidenceRepository extends JpaRepository<InvestigationEvidence, UUID> {
    List<InvestigationEvidence> findByInvestigationCase_IdOrderByCreatedAtAsc(UUID caseId);
}
