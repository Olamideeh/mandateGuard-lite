package com.example.mandateguard.controller;

import com.example.mandateguard.dto.ApprovalDecisionRequest;
import com.example.mandateguard.dto.PaymentApprovalResponse;
import com.example.mandateguard.service.PaymentApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class PaymentApprovalController {

    private final PaymentApprovalService approvalService;

    @GetMapping("/pending")
    public ResponseEntity<List<PaymentApprovalResponse>>
    getPendingApprovals(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                approvalService.getPendingApprovals(principalId)
        );
    }

    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<PaymentApprovalResponse> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID approvalId,
            @Valid @RequestBody
            ApprovalDecisionRequest request
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                approvalService.approve(
                        principalId,
                        approvalId,
                        request
                )
        );
    }

    @PostMapping("/{approvalId}/reject")
    public ResponseEntity<PaymentApprovalResponse> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID approvalId,
            @Valid @RequestBody
            ApprovalDecisionRequest request
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                approvalService.reject(
                        principalId,
                        approvalId,
                        request
                )
        );
    }
}