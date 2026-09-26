package com.example.mandateguard.dto;

import com.example.mandateguard.enums.DecisionReasonCode;
import com.example.mandateguard.enums.PaymentDecision;
import com.example.mandateguard.enums.PaymentRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestResponse(

        UUID id,
        String paymentReference,
        String idempotencyKey,
        String nonce,
        BigDecimal amount,
        String currencyCode,
        String merchantIdentifier,
        String merchantName,
        String merchantCategoryCode,
        String merchantCountryCode,
        boolean merchantVerified,
        String description,
        PaymentRequestStatus status,
        PaymentDecision decision,
        DecisionReasonCode reasonCode,
        String decisionExplanation,
        UUID mandateId,
        UUID agentId,
        Instant agentTimestamp,
        Instant decisionAt,
        Instant approvalAt,
        Instant executionAt,
        String providerReference,
        Instant createdAt
) {
}