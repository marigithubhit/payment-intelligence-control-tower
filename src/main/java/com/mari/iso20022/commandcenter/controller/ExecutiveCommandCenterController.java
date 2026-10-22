package com.mari.iso20022.commandcenter.controller;

import com.mari.iso20022.commandcenter.dto.CommandCenterDtos.*;
import com.mari.iso20022.commandcenter.service.CorrectiveActionService;
import com.mari.iso20022.commandcenter.service.ExecutiveCockpitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/command-center", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExecutiveCommandCenterController {

    private final ExecutiveCockpitService cockpitService;
    private final CorrectiveActionService actionService;

    public ExecutiveCommandCenterController(ExecutiveCockpitService cockpitService,
                                            CorrectiveActionService actionService) {
        this.cockpitService = cockpitService;
        this.actionService = actionService;
    }

    @GetMapping("/overview")
    public ExecutiveOverview getOverview() {
        return cockpitService.getOverview();
    }

    @GetMapping("/regional")
    public RegionalIntelligence getRegionalIntelligence() {
        return cockpitService.getRegionalIntelligence();
    }

    @GetMapping("/exception-patterns")
    public List<ExceptionPattern> getExceptionPatterns() {
        return cockpitService.getExceptionPatterns();
    }

    @GetMapping("/priority-queue")
    public List<PriorityQueueItem> getPriorityQueue() {
        return cockpitService.getPrioritizedQueue();
    }

    @GetMapping("/decision-brief/{transactionId}")
    public ExecutiveDecisionBrief getDecisionBrief(@PathVariable String transactionId) {
        return cockpitService.getDecisionBrief(transactionId);
    }

    @GetMapping("/actions")
    public List<CorrectiveActionResponse> getAllActions() {
        return actionService.getAllActions();
    }

    @GetMapping("/actions/transaction/{transactionId}")
    public List<CorrectiveActionResponse> getActionsForTransaction(@PathVariable String transactionId) {
        return actionService.getActionsForTransaction(transactionId);
    }

    @GetMapping("/actions/{actionId}")
    public CorrectiveActionResponse getAction(@PathVariable UUID actionId) {
        return actionService.getAction(actionId);
    }

    @PostMapping(value = "/actions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CorrectiveActionResponse createAction(@Valid @RequestBody CreateActionRequest request) {
        return actionService.createAction(request);
    }

    @PostMapping(value = "/actions/{actionId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CorrectiveActionResponse updateActionStatus(@PathVariable UUID actionId,
                                                       @Valid @RequestBody ActionStatusUpdateRequest request) {
        return actionService.updateStatus(actionId, request);
    }

    @PostMapping(value = "/actions/{actionId}/approval", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CorrectiveActionResponse approveAction(@PathVariable UUID actionId,
                                                  @Valid @RequestBody ActionApprovalRequest request) {
        return actionService.processApproval(actionId, request);
    }

    @PostMapping(value = "/actions/{actionId}/notes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CorrectiveActionResponse addActionNote(@PathVariable UUID actionId,
                                                  @Valid @RequestBody ActionNoteRequest request) {
        return actionService.addNote(actionId, request);
    }

    @PostMapping(value = "/actions/{actionId}/outcome", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CorrectiveActionResponse updateOutcome(@PathVariable UUID actionId,
                                                  @Valid @RequestBody ActionOutcomeUpdateRequest request) {
        return actionService.updateOutcome(actionId, request);
    }
}
