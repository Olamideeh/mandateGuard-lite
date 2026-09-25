package com.example.mandateguard.service;

import com.example.mandateguard.dto.*;
import com.example.mandateguard.entity.Principal;
import com.example.mandateguard.exception.DuplicateResourceException;
import com.example.mandateguard.exception.InvalidCredentialsException;
import com.example.mandateguard.repository.PrincipalRepository;
import com.example.mandateguard.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthenticationService {

    private static final Set<String> ISO_COUNTRY_CODES =
            Set.copyOf(
                    Arrays.asList(Locale.getISOCountries())
            );

    private final PrincipalRepository principalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthenticationService(
            PrincipalRepository principalRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.principalRepository = principalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public PrincipalResponse register(
            RegisterPrincipalRequest request
    ) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        String countryCode = request.countryCode()
                .trim()
                .toUpperCase();

        if (!ISO_COUNTRY_CODES.contains(countryCode)) {
            throw new IllegalArgumentException(
                    "Country code is not a valid ISO 3166 code"
            );
        }

        if (principalRepository
                .existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "An account with this email already exists"
            );
        }

        Principal principal = Principal.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .passwordHash(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .type(request.type())
                .countryCode(countryCode)
                .active(true)
                .build();

        return mapToResponse(
                principalRepository.save(principal)
        );
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse login(
            LoginRequest request
    ) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        Principal principal = principalRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .filter(Principal::isActive)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                principal.getPasswordHash()
        )) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        JwtTokenService.GeneratedToken generatedToken =
                jwtTokenService.generateToken(principal);

        return new AuthenticationResponse(
                generatedToken.value(),
                "Bearer",
                generatedToken.expiresAt(),
                principal.getId(),
                principal.getEmail()
        );
    }

    private PrincipalResponse mapToResponse(
            Principal principal
    ) {
        return new PrincipalResponse(
                principal.getId(),
                principal.getName(),
                principal.getEmail(),
                principal.getType(),
                principal.getCountryCode(),
                principal.isActive(),
                principal.getCreatedAt()
        );
    }
}