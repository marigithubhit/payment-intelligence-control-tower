package com.mari.iso20022.commandcenter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "corrective_action_note")
public class CorrectiveActionNote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    @JsonIgnore
    private CorrectiveAction action;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false, length = 64)
    private String role;

    @Column(nullable = false, length = 2000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public CorrectiveActionNote() {}

    public CorrectiveActionNote(UUID id, CorrectiveAction action, String author, String role, String note, OffsetDateTime createdAt) {
        this.id = id;
        this.action = action;
        this.author = author;
        this.role = role;
        this.note = note;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CorrectiveAction getAction() { return action; }
    public void setAction(CorrectiveAction action) { this.action = action; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
