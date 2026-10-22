package com.mari.iso20022.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {
    @Id
    private UUID id;
    @Column(name = "document_code", nullable = false, length = 100)
    private String documentCode;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;
    @Column(nullable = false, length = 64)
    private String version;
    @Column(nullable = false, length = 255)
    private String source;
    @Column(length = 2000)
    private String description;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected KnowledgeDocument() { }

    public UUID getId() { return id; }
    public String getDocumentCode() { return documentCode; }
    public String getTitle() { return title; }
    public String getDocumentType() { return documentType; }
    public String getVersion() { return version; }
    public String getSource() { return source; }
    public String getDescription() { return description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
