package com.example.mandateguard.controller;

import com.example.mandateguard.dto.CreateMandateRequest;
import com.example.mandateguard.dto.MandateResponse;
import com.example.mandateguard.service.MandateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mandates")
@RequiredArgsConstructor
public class MandateController {

    private final MandateService mandateService;

    @PostMapping
    public ResponseEntity<MandateResponse> createMandate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMandateRequest request
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        MandateResponse response =
                mandateService.createMandate(principalId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{mandateId}/activate")
    public ResponseEntity<MandateResponse> activateMandate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID mandateId
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                mandateService.activateMandate(
                        principalId,
                        mandateId
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<MandateResponse>> getMandates(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                mandateService.getMandates(principalId)
        );
    }
    @PostMapping("/{mandateId}/suspend")
    public ResponseEntity<MandateResponse> suspendMandate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID mandateId
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                mandateService.suspendMandate(
                        principalId,
                        mandateId
                )
        );
    }
    @PostMapping("/{mandateId}/resume")
    public ResponseEntity<MandateResponse> resumeMandate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID mandateId
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                mandateService.resumeMandate(
                        principalId,
                        mandateId
                )
        );
    }
    @PostMapping("/{mandateId}/revoke")
    public ResponseEntity<MandateResponse> revokeMandate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID mandateId
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                mandateService.revokeMandate(
                        principalId,
                        mandateId
                )
        );
    }
}