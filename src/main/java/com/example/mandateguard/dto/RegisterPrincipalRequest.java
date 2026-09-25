package com.example.mandateguard.dto;

import com.example.mandateguard.enums.PrincipalType;
import jakarta.validation.constraints.*;

public record RegisterPrincipalRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(
                min = 10,
                max = 72,
                message = "Password must contain 10 to 72 characters"
        )
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain uppercase, lowercase and number"
        )
        String password,

        @NotNull(message = "Principal type is required")
        PrincipalType type,

        @NotBlank(message = "Country code is required")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must contain two letters"
        )
        String countryCode
) {
}