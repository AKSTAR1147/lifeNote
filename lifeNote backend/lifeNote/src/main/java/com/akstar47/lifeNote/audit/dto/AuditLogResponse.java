package com.akstar47.lifeNote.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        AuditAction action,
        String detail,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
