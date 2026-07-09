package com.akstar47.lifeNote.note.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record NoteVersionResponse(
        UUID id,
        UUID noteId,
        int versionNumber,
        Integer parentVersionNumber,
        String title,
        String content,
        String category,
        Set<String> tags,
        String commitMessage,
        Instant createdAt
) {
}
