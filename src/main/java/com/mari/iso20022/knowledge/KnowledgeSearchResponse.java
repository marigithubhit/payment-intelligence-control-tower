package com.mari.iso20022.knowledge;

import java.util.List;

public record KnowledgeSearchResponse(String query, List<KnowledgeReference> results) {
    public KnowledgeSearchResponse {
        results = List.copyOf(results);
    }
}
