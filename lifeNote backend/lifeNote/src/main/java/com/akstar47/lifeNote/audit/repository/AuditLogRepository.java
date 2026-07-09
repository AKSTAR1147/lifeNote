package com.akstar47.lifeNote.audit.repository;

import com.akstar47.lifeNote.audit.entity.AuditLog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);
    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
