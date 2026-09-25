package com.example.mandateguard.controller;

import com.example.mandateguard.dto.*;
import com.example.mandateguard.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(
            AgentService agentService
    ) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<AgentResponse> registerAgent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody
            RegisterAgentRequest request
    ) {
        AgentResponse response =
                agentService.registerAgent(
                        principalId(jwt),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{agentId}/activate")
    public ResponseEntity<AgentResponse> activateAgent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID agentId
    ) {
        return ResponseEntity.ok(
                agentService.activateAgent(
                        principalId(jwt),
                        agentId
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<AgentResponse>> getAgents(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                agentService.getAgents(
                        principalId(jwt)
                )
        );
    }

    private UUID principalId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}