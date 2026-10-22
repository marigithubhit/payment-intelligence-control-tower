package com.mari.iso20022.commandcenter.repository;

import com.mari.iso20022.commandcenter.entity.CorrectiveActionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorrectiveActionNoteRepository extends JpaRepository<CorrectiveActionNote, UUID> {
    List<CorrectiveActionNote> findByActionIdOrderByCreatedAtAsc(UUID actionId);
}
