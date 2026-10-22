package com.mari.iso20022.knowledge;

import java.util.UUID;

public record KnowledgeReference(UUID knowledgeId, String documentCode, String title, String topic,
                                 String content, String source, String version) {
    static KnowledgeReference from(KnowledgeChunk chunk) {
        KnowledgeDocument document = chunk.getDocument();
        return new KnowledgeReference(chunk.getId(), document.getDocumentCode(), document.getTitle(),
                chunk.getTopic(), chunk.getContent(), document.getSource(), document.getVersion());
    }
}
