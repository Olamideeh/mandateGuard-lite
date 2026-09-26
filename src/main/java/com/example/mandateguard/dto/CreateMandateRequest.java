package com.example.mandateguard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateMandateRequest(

        @NotNull(message = "Agent ID is required")
        UUID agentId,

        @NotBlank(message = "Mandate name is required")
        String name,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must contain 3 characters")
        String currencyCode,

        @NotNull(message = "Maximum single amount is required")
        @DecimalMin(value = "0.01", message = "Maximum single amount must be greater than zero")
        BigDecimal maxSingleAmount,

        @NotNull(message = "Total budget is required")
        @DecimalMin(value = "0.01", message = "Total budget must be greater than zero")
        BigDecimal totalBudget,

        @DecimalMin(value = "0.01", message = "Approval threshold must be greater than zero")
        BigDecimal approvalThreshold,

        @NotNull(message = "Maximum transaction count is required")
        @Positive(message = "Maximum transaction count must be greater than zero")
        Integer maxTransactions,

        boolean allowAnyVerifiedMerchant,

        Set<String> allowedCountryCodes,

        Set<String> allowedMerchantIds,

        Set<String> allowedMerchantCategoryCodes,

        @NotNull(message = "Valid-from time is required")
        Instant validFrom,

        @NotNull(message = "Expiration time is required")
        Instant expiresAt
) {
}