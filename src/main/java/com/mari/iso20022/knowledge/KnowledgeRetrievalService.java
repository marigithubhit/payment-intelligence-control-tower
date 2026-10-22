package com.mari.iso20022.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeRetrievalService {
    private final KnowledgeRepository repository;

    public KnowledgeRetrievalService(KnowledgeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        String query = request.q().trim();
        if (query.isEmpty()) return new KnowledgeSearchResponse(request.q(), List.of());
        List<KnowledgeReference> results = repository.search(query).stream()
                .map(KnowledgeReference::from)
                .toList();
        return new KnowledgeSearchResponse(query, results);
    }
}
