package com.example.mandateguard.dto;

import com.example.mandateguard.enums.PrincipalType;

import java.time.Instant;
import java.util.UUID;

public record PrincipalResponse(
        UUID id,
        String name,
        String email,
        PrincipalType type,
        String countryCode,
        boolean active,
        Instant createdAt
) {
}