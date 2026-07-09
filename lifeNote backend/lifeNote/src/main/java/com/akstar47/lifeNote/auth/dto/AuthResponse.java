package com.akstar47.lifeNote.auth.dto;

import com.akstar47.lifeNote.user.dto.UserProfileResponse;


public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInMinutes,
        UserProfileResponse user
) {
}
