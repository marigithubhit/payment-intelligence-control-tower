package com.mari.iso20022.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;
    @Column(name = "chunk_code", nullable = false, length = 100)
    private String chunkCode;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(nullable = false, length = 100)
    private String topic;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String keywords;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected KnowledgeChunk() { }

    public UUID getId() { return id; }
    public KnowledgeDocument getDocument() { return document; }
    public String getChunkCode() { return chunkCode; }
    public String getContent() { return content; }
    public String getTopic() { return topic; }
    public String getKeywords() { return keywords; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
