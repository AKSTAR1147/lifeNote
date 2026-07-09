package com.akstar47.lifeNote.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        Instant createdAt
) {
}
