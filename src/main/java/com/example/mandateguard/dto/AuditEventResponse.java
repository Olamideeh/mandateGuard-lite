package com.example.mandateguard.dto;

import com.example.mandateguard.enums.AuditActorType;
import com.example.mandateguard.enums.AuditEventType;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(

        UUID id,
        String correlationId,
        AuditActorType actorType,
        String actorId,
        AuditEventType eventType,
        String entityType,
        String entityId,
        String previousState,
        String newState,
        String details,
        Instant occurredAt
) {
}