package com.akstar47.lifeNote.audit.mapper;

import com.akstar47.lifeNote.audit.dto.AuditLogResponse;
import com.akstar47.lifeNote.audit.entity.AuditLog;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
