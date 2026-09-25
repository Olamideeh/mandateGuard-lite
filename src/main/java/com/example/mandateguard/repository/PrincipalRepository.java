package com.example.mandateguard.repository;

import com.example.mandateguard.entity.Principal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrincipalRepository
        extends JpaRepository<Principal, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Principal> findByEmailIgnoreCase(String email);
}