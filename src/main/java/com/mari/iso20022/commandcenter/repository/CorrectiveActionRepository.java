package com.mari.iso20022.commandcenter.repository;

import com.mari.iso20022.commandcenter.entity.CorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, UUID> {
    List<CorrectiveAction> findByTransactionIdOrderByCreatedAtDesc(String transactionId);
    List<CorrectiveAction> findByCaseIdOrderByCreatedAtDesc(UUID caseId);
    List<CorrectiveAction> findAllByOrderByCreatedAtDesc();
    List<CorrectiveAction> findByStatusOrderByCreatedAtDesc(String status);
    List<CorrectiveAction> findByAssignedTeamOrderByCreatedAtDesc(String assignedTeam);
}
