package com.example.mandateguard.dto;

import com.example.mandateguard.enums.MandateStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MandateResponse(

        UUID id,
        String mandateReference,
        String name,
        MandateStatus status,
        String currencyCode,
        BigDecimal maxSingleAmount,
        BigDecimal totalBudget,
        BigDecimal consumedAmount,
        BigDecimal remainingBudget,
        BigDecimal approvalThreshold,
        Integer maxTransactions,
        Integer usedTransactionCount,
        boolean allowAnyVerifiedMerchant,
        Set<String> allowedCountryCodes,
        Set<String> allowedMerchantIds,
        Set<String> allowedMerchantCategoryCodes,
        UUID agentId,
        String agentName,
        Instant validFrom,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}