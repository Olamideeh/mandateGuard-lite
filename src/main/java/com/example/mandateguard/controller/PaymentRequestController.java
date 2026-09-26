package com.example.mandateguard.controller;

import com.example.mandateguard.dto.CreatePaymentRequest;
import com.example.mandateguard.dto.PaymentRequestResponse;
import com.example.mandateguard.service.PaymentExecutionService;
import com.example.mandateguard.service.PaymentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.mandateguard.service.PaymentExecutionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-requests")
@RequiredArgsConstructor
public class PaymentRequestController {

    private final PaymentExecutionService paymentExecutionService;
    private final PaymentRequestService paymentRequestService;


    @PostMapping
    public ResponseEntity<PaymentRequestResponse> submitPayment(
            @RequestHeader("X-Agent-Id") UUID agentId,
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @RequestHeader("X-Nonce") String nonce,
            @RequestHeader("X-Signature") String signature,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentRequestResponse response =
                paymentRequestService.submitPayment(
                        agentId,
                        idempotencyKey,
                        nonce,
                        signature,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @PostMapping("/{paymentRequestId}/execute")
    public ResponseEntity<PaymentRequestResponse> executePayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentRequestId
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                paymentExecutionService.execute(
                        principalId,
                        paymentRequestId
                )
        );
    }
    @GetMapping
    public ResponseEntity<java.util.List<PaymentRequestResponse>>
    getPayments(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID principalId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                paymentRequestService.getPayments(principalId)
        );
    }
}