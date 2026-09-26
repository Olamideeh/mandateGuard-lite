package com.example.mandateguard.controller;

import com.example.mandateguard.dto.CreatePaymentRequest;
import com.example.mandateguard.dto.PaymentRequestResponse;
import com.example.mandateguard.service.PaymentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-requests")
@RequiredArgsConstructor
public class PaymentRequestController {

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
}