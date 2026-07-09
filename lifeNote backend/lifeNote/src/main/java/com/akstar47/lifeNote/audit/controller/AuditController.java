package com.akstar47.lifeNote.audit.controller;

import com.akstar47.lifeNote.audit.dto.AuditLogResponse;
import com.akstar47.lifeNote.audit.service.AuditService;
import com.akstar47.lifeNote.user.service.AuthenticatedUserService;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class AuditController {

    private final AuthenticatedUserService authenticatedUserService;
    private final AuditService auditService;

    public AuditController(AuthenticatedUserService authenticatedUserService, AuditService auditService) {
        this.authenticatedUserService = authenticatedUserService;
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public List<AuditLogResponse> myAuditTrail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return auditService.latestFor(authenticatedUserService.requireUser(userDetails), limit);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogResponse> allAuditLogs(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return auditService.latestAll(limit);
    }
}
