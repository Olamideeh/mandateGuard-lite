package com.example.mandateguard.service;

import com.example.mandateguard.dto.AuditEventResponse;
import com.example.mandateguard.entity.AuditEvent;
import com.example.mandateguard.enums.AuditActorType;
import com.example.mandateguard.enums.AuditEventType;
import com.example.mandateguard.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID principalId,
            String correlationId,
            AuditActorType actorType,
            String actorId,
            AuditEventType eventType,
            String entityType,
            String entityId,
            String previousState,
            String newState,
            String details
    ) {
        AuditEvent event = AuditEvent.builder()
                .principalId(principalId)
                .correlationId(correlationId)
                .actorType(actorType)
                .actorId(actorId)
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .previousState(previousState)
                .newState(newState)
                .details(details)
                .build();

        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getEvents(
            UUID principalId
    ) {
        return auditEventRepository
                .findAllByPrincipalIdOrderByOccurredAtDesc(
                        principalId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getCorrelationTimeline(
            UUID principalId,
            String correlationId
    ) {
        return auditEventRepository
                .findAllByPrincipalIdAndCorrelationIdOrderByOccurredAtAsc(
                        principalId,
                        correlationId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditEventResponse toResponse(
            AuditEvent event
    ) {
        return new AuditEventResponse(
                event.getId(),
                event.getCorrelationId(),
                event.getActorType(),
                event.getActorId(),
                event.getEventType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getPreviousState(),
                event.getNewState(),
                event.getDetails(),
                event.getOccurredAt()
        );
    }
}