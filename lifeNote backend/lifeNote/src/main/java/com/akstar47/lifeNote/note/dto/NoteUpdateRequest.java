package com.akstar47.lifeNote.note.dto;

import jakarta.validation.constraints.Size;
import java.util.Set;

public record NoteUpdateRequest(
        @Size(max = 240, message = "Title cannot be longer than 240 characters")
        String title,

        String content,

        @Size(max = 120, message = "Category cannot be longer than 120 characters")
        String category,

        Set<@Size(max = 80, message = "Tags cannot be longer than 80 characters") String> tags,

        @Size(max = 500, message = "Commit message cannot be longer than 500 characters")
        String commitMessage,

        Boolean pinned,
        Boolean favorite
) {
}
