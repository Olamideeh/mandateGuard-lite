package com.example.mandateguard.repository;

import com.example.mandateguard.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findAllByCorrelationIdOrderByOccurredAtAsc(
            String correlationId
    );

    List<AuditEvent>
    findAllByEntityTypeAndEntityIdOrderByOccurredAtAsc(
            String entityType,
            String entityId
    );
}