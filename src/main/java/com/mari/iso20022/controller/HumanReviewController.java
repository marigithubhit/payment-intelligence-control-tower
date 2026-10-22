package com.mari.iso20022.controller;

import com.mari.iso20022.investigation.HumanReviewService;
import com.mari.iso20022.investigation.dto.InvestigationReviewRequest;
import com.mari.iso20022.investigation.dto.InvestigationReviewResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investigations/cases/{caseId}")
public class HumanReviewController {
    private final HumanReviewService humanReviewService;

    public HumanReviewController(HumanReviewService humanReviewService) {
        this.humanReviewService = humanReviewService;
    }

    @PostMapping("/review")
    public InvestigationReviewResponse submitReview(@PathVariable UUID caseId,
                                                    @Valid @RequestBody InvestigationReviewRequest request) {
        return humanReviewService.submit(caseId, request);
    }

    @GetMapping("/reviews")
    public List<InvestigationReviewResponse> reviewHistory(@PathVariable UUID caseId) {
        return humanReviewService.history(caseId);
    }
}
