package com.akstar47.lifeNote.note.dto;

import java.util.List;
import java.util.UUID;

public record DiffResponse(
        UUID noteId,
        int fromVersion,
        int toVersion,
        List<DiffLineResponse> changes
) {
}
