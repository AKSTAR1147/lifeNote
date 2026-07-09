package com.akstar47.lifeNote.note.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "note_versions")
public class NoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(nullable = false)
    private int versionNumber;

    private Integer parentVersionNumber;

    @Column(nullable = false, length = 240)
    private String titleSnapshot;

    @Lob
    @Column(nullable = false)
    private String contentSnapshot;

    @Column(length = 120)
    private String categorySnapshot;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "note_version_tags", joinColumns = @JoinColumn(name = "note_version_id"))
    @Column(name = "tag", length = 80)
    private Set<String> tagsSnapshot = new LinkedHashSet<>();

    @Column(length = 500)
    private String commitMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NoteVersion() {
    }

    public NoteVersion(
            Note note,
            int versionNumber,
            Integer parentVersionNumber,
            String titleSnapshot,
            String contentSnapshot,
            String categorySnapshot,
            Set<String> tagsSnapshot,
            String commitMessage
    ) {
        this.note = note;
        this.versionNumber = versionNumber;
        this.parentVersionNumber = parentVersionNumber;
        this.titleSnapshot = titleSnapshot;
        this.contentSnapshot = contentSnapshot;
        this.categorySnapshot = categorySnapshot;
        if (tagsSnapshot != null) {
            this.tagsSnapshot.addAll(tagsSnapshot);
        }
        this.commitMessage = commitMessage;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public Integer getParentVersionNumber() {
        return parentVersionNumber;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public String getCategorySnapshot() {
        return categorySnapshot;
    }

    public Set<String> getTagsSnapshot() {
        return tagsSnapshot;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
