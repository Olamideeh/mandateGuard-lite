package com.example.mandateguard.dto;

import com.example.mandateguard.enums.AgentStatus;

import java.time.Instant;
import java.util.UUID;

public record AgentResponse(
        UUID id,
        String externalAgentId,
        String name,
        String provider,
        AgentStatus status,
        String publicKeyFingerprint,
        UUID principalId,
        Instant createdAt
) {
}