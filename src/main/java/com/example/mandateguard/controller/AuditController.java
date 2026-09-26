package com.example.mandateguard.controller;

import com.example.mandateguard.dto.AuditEventResponse;
import com.example.mandateguard.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditEventResponse>> getEvents(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                auditService.getEvents(principalId)
        );
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<AuditEventResponse>>
    getCorrelationTimeline(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String correlationId
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                auditService.getCorrelationTimeline(
                        principalId,
                        correlationId
                )
        );
    }
}