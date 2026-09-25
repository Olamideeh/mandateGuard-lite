package com.example.mandateguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAgentRequest(

        @NotBlank(message = "External agent ID is required")
        @Size(max = 100)
        String externalAgentId,

        @NotBlank(message = "Agent name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Provider is required")
        @Size(max = 100)
        String provider,

        @NotBlank(message = "Public key is required")
        String publicKeyPem
) {
}