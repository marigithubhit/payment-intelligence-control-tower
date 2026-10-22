package com.mari.iso20022.knowledge;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeRetrievalService retrievalService;

    public KnowledgeController(KnowledgeRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @GetMapping("/search")
    public KnowledgeSearchResponse search(@Valid @ModelAttribute KnowledgeSearchRequest request) {
        return retrievalService.search(request);
    }
}
