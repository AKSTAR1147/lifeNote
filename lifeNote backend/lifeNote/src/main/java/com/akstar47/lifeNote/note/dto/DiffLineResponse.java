package com.akstar47.lifeNote.note.dto;

public record DiffLineResponse(
        ChangeType changeType,
        Integer oldLineNumber,
        Integer newLineNumber,
        String oldText,
        String newText
) {
}
