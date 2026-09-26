package com.example.mandateguard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreatePaymentRequest(

        @NotNull(message = "Mandate ID is required")
        UUID mandateId,

        @NotBlank(message = "Payment reference is required")
        String paymentReference,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,

        @NotBlank(message = "Currency code is required")
        @Size(
                min = 3,
                max = 3,
                message = "Currency code must contain 3 characters"
        )
        String currencyCode,

        @NotBlank(message = "Merchant identifier is required")
        String merchantIdentifier,

        @NotBlank(message = "Merchant name is required")
        String merchantName,

        @NotBlank(message = "Merchant category code is required")
        @Size(
                min = 4,
                max = 4,
                message = "Merchant category code must contain 4 characters"
        )
        String merchantCategoryCode,

        @NotBlank(message = "Merchant country code is required")
        @Size(
                min = 2,
                max = 2,
                message = "Merchant country code must contain 2 characters"
        )
        String merchantCountryCode,

        boolean merchantVerified,

        String description,

        @NotNull(message = "Agent timestamp is required")
        Instant agentTimestamp
) {
}