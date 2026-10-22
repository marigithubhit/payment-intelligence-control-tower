package com.mari.iso20022.commandcenter.service;

import com.mari.iso20022.commandcenter.dto.CommandCenterDtos.*;
import com.mari.iso20022.commandcenter.entity.CorrectiveAction;
import com.mari.iso20022.commandcenter.entity.CorrectiveActionNote;
import com.mari.iso20022.commandcenter.repository.CorrectiveActionNoteRepository;
import com.mari.iso20022.commandcenter.repository.CorrectiveActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CorrectiveActionService {

    private final CorrectiveActionRepository actionRepository;
    private final CorrectiveActionNoteRepository noteRepository;

    public CorrectiveActionService(CorrectiveActionRepository actionRepository,
                                  CorrectiveActionNoteRepository noteRepository) {
        this.actionRepository = actionRepository;
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public List<CorrectiveActionResponse> getAllActions() {
        return actionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CorrectiveActionResponse> getActionsForTransaction(String transactionId) {
        return actionRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CorrectiveActionResponse getAction(UUID actionId) {
        CorrectiveAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NoSuchElementException("Corrective action not found: " + actionId));
        return toResponse(action);
    }

    @Transactional
    public CorrectiveActionResponse createAction(CreateActionRequest req) {
        OffsetDateTime now = OffsetDateTime.now();
        UUID id = UUID.randomUUID();
        CorrectiveAction action = new CorrectiveAction(
                id,
                req.caseId(),
                req.transactionId(),
                req.title(),
                req.description(),
                req.actionType(),
                "PROPOSED",
                req.assignedTeam(),
                req.assignedRole(),
                req.assignedTo(),
                req.humanApprovalRequired(),
                req.humanApprovalRequired() ? "PENDING" : "NOT_REQUIRED",
                req.targetMetric(),
                req.baselineMetricValue(),
                now
        );
        actionRepository.save(action);
        return toResponse(action);
    }

    @Transactional
    public CorrectiveActionResponse updateStatus(UUID actionId, ActionStatusUpdateRequest req) {
        CorrectiveAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NoSuchElementException("Corrective action not found: " + actionId));

        action.setStatus(req.status());
        action.setUpdatedAt(OffsetDateTime.now());
        if ("COMPLETED".equalsIgnoreCase(req.status())) {
            action.setCompletedAt(OffsetDateTime.now());
        }
        actionRepository.save(action);
        return toResponse(action);
    }

    @Transactional
    public CorrectiveActionResponse processApproval(UUID actionId, ActionApprovalRequest req) {
        CorrectiveAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NoSuchElementException("Corrective action not found: " + actionId));

        OffsetDateTime now = OffsetDateTime.now();
        action.setApprovalStatus(req.decision().toUpperCase());
        action.setApprovedBy(req.reviewer());
        action.setApprovedAt(now);
        action.setApprovalNotes(req.notes());
        action.setUpdatedAt(now);

        if ("APPROVED".equalsIgnoreCase(req.decision())) {
            action.setStatus("IN_PROGRESS");
        } else if ("REJECTED".equalsIgnoreCase(req.decision())) {
            action.setStatus("REJECTED");
        }

        actionRepository.save(action);
        return toResponse(action);
    }

    @Transactional
    public CorrectiveActionResponse addNote(UUID actionId, ActionNoteRequest req) {
        CorrectiveAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NoSuchElementException("Corrective action not found: " + actionId));

        OffsetDateTime now = OffsetDateTime.now();
        CorrectiveActionNote note = new CorrectiveActionNote(
                UUID.randomUUID(),
                action,
                req.author(),
                req.role(),
                req.note(),
                now
        );
        noteRepository.save(note);
        action.setUpdatedAt(now);
        actionRepository.save(action);
        return toResponse(action);
    }

    @Transactional
    public CorrectiveActionResponse updateOutcome(UUID actionId, ActionOutcomeUpdateRequest req) {
        CorrectiveAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NoSuchElementException("Corrective action not found: " + actionId));

        OffsetDateTime now = OffsetDateTime.now();
        action.setPostActionMetricValue(req.postActionMetricValue());
        action.setOutcomeStatus(req.outcomeStatus());
        action.setOutcomeNotes(req.outcomeNotes());
        action.setUpdatedAt(now);
        actionRepository.save(action);
        return toResponse(action);
    }

    private CorrectiveActionResponse toResponse(CorrectiveAction a) {
        List<ActionNoteResponse> noteResponses = noteRepository.findByActionIdOrderByCreatedAtAsc(a.getId())
                .stream()
                .map(n -> new ActionNoteResponse(n.getId(), n.getAuthor(), n.getRole(), n.getNote(), n.getCreatedAt()))
                .toList();

        return new CorrectiveActionResponse(
                a.getId(),
                a.getCaseId(),
                a.getTransactionId(),
                a.getTitle(),
                a.getDescription(),
                a.getActionType(),
                a.getStatus(),
                a.getAssignedTeam(),
                a.getAssignedRole(),
                a.getAssignedTo(),
                a.isHumanApprovalRequired(),
                a.getApprovalStatus(),
                a.getApprovedBy(),
                a.getApprovedAt(),
                a.getApprovalNotes(),
                a.getTargetMetric(),
                a.getBaselineMetricValue(),
                a.getPostActionMetricValue(),
                a.getOutcomeStatus(),
                a.getOutcomeNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getCompletedAt(),
                noteResponses
        );
    }
}
