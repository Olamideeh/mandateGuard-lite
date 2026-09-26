package com.example.mandateguard.dto;

import com.example.mandateguard.enums.ApprovalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentApprovalResponse(

        UUID approvalId,
        UUID paymentRequestId,
        String paymentReference,
        BigDecimal amount,
        String currencyCode,
        String merchantName,
        ApprovalStatus status,
        String decisionNote,
        Instant requestedAt,
        Instant expiresAt,
        Instant decidedAt
) {
}