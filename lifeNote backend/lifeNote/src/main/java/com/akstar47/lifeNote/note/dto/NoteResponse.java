package com.akstar47.lifeNote.note.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String title,
        String content,
        String category,
        Set<String> tags,
        boolean archived,
        boolean pinned,
        boolean favorite,
        int currentVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
