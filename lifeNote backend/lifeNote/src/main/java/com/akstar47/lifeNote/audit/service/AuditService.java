package com.akstar47.lifeNote.audit.service;

import com.akstar47.lifeNote.audit.dto.AuditAction;
import com.akstar47.lifeNote.audit.dto.AuditLogResponse;
import com.akstar47.lifeNote.audit.entity.AuditLog;
import com.akstar47.lifeNote.audit.mapper.AuditMapper;
import com.akstar47.lifeNote.audit.repository.AuditLogRepository;
import com.akstar47.lifeNote.user.entity.AppUser;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditMapper auditMapper;

    public AuditService(AuditLogRepository auditLogRepository, AuditMapper auditMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditMapper = auditMapper;
    }

    @Transactional
    public void record(AppUser actor, AuditAction action, String detail, HttpServletRequest request) {
        String ipAddress = request == null ? null : clientIp(request);
        String userAgent = request == null ? null : truncate(request.getHeader("User-Agent"), 500);
        auditLogRepository.save(new AuditLog(actor, action, truncate(detail, 1000), ipAddress, userAgent));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> latestFor(AppUser actor, int limit) {
        int sanitizedLimit = Math.max(1, Math.min(limit, 100));
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actor.getId(), PageRequest.of(0, sanitizedLimit))
                .stream()
                .map(auditMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> latestAll(int limit) {
        int sanitizedLimit = Math.max(1, Math.min(limit, 100));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, sanitizedLimit))
                .stream()
                .map(auditMapper::toResponse)
                .toList();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return truncate(forwardedFor.split(",")[0].trim(), 80);
        }
        return truncate(request.getRemoteAddr(), 80);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
