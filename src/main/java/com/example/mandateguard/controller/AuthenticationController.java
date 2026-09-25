package com.example.mandateguard.controller;

import com.example.mandateguard.dto.*;
import com.example.mandateguard.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService =
                authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<PrincipalResponse> register(
            @Valid @RequestBody
            RegisterPrincipalRequest request
    ) {
        PrincipalResponse response =
                authenticationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authenticationService.login(request)
        );
    }
}