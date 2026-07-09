package com.akstar47.lifeNote.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 120, message = "Display name must be between 2 and 120 characters")
        String displayName,

        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {
}
