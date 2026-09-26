package com.example.mandateguard.repository;

import com.example.mandateguard.entity.AiAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiAgentRepository
        extends JpaRepository<AiAgent, UUID> {

    boolean existsByPrincipal_IdAndExternalAgentId(
            UUID principalId,
            String externalAgentId
    );

    Optional<AiAgent> findByIdAndPrincipal_Id(
            UUID agentId,
            UUID principalId
    );

    Optional<AiAgent> findByPublicKeyFingerprint(
            String fingerprint
    );

    List<AiAgent> findAllByPrincipal_Id(UUID principalId);

}