package com.example.mandateguard.security;

import com.example.mandateguard.entity.Principal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtTokenService(
            JwtEncoder jwtEncoder,

            @Value("${mandateguard.jwt.expiration-minutes}")
            long expirationMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public GeneratedToken generateToken(
            Principal principal
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
                expirationMinutes,
                ChronoUnit.MINUTES
        );

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mandateguard")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(principal.getId().toString())
                .claim("email", principal.getEmail())
                .claim("role", "PRINCIPAL")
                .claim(
                        "principalType",
                        principal.getType().name()
                )
                .build();

        String token = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();

        return new GeneratedToken(token, expiresAt);
    }

    public record GeneratedToken(
            String value,
            Instant expiresAt
    ) {
    }
}