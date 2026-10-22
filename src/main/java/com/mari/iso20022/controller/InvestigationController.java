package com.mari.iso20022.controller;

import com.mari.iso20022.investigation.InvestigationService;
import com.mari.iso20022.investigation.HistoricalAnalysisService;
import com.mari.iso20022.investigation.dto.HistoricalAnalysisResult;
import com.mari.iso20022.investigation.dto.InvestigationResult;
import com.mari.iso20022.investigation.InvestigationPersistenceService;
import com.mari.iso20022.investigation.dto.InvestigationCaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investigations")
public class InvestigationController {
    private final InvestigationService investigationService;
    private final InvestigationPersistenceService persistenceService;
    private final HistoricalAnalysisService historicalAnalysisService;

    public InvestigationController(InvestigationService investigationService,
                                   InvestigationPersistenceService persistenceService,
                                   HistoricalAnalysisService historicalAnalysisService) {
        this.investigationService = investigationService;
        this.persistenceService = persistenceService;
        this.historicalAnalysisService = historicalAnalysisService;
    }

    @GetMapping("/{transactionId}")
    public InvestigationResult investigate(@PathVariable String transactionId) {
        return investigationService.investigate(transactionId);
    }

    @GetMapping("/cases/{caseId}")
    public InvestigationCaseResponse getCase(@PathVariable UUID caseId) {
        return persistenceService.getCase(caseId);
    }

    @GetMapping("/{transactionId}/history")
    public HistoricalAnalysisResult getHistory(@PathVariable String transactionId) {
        return historicalAnalysisService.analyzeByTransactionId(transactionId);
    }
}
