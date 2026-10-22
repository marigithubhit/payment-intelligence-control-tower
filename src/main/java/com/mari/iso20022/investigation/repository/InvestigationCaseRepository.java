package com.mari.iso20022.investigation.repository;

import com.mari.iso20022.investigation.entity.InvestigationCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface InvestigationCaseRepository extends JpaRepository<InvestigationCase, UUID> {
    List<InvestigationCase> findByTransactionIdOrderByCreatedAtDesc(String transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InvestigationCase c where c.id = :caseId")
    java.util.Optional<InvestigationCase> findByIdForUpdate(@Param("caseId") UUID caseId);
}
