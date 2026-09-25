package com.example.mandateguard.repository;

import com.example.mandateguard.entity.AgentPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentPaymentRequestRepository
        extends JpaRepository<AgentPaymentRequest, UUID> {

    boolean existsByReference(String reference);

    Optional<AgentPaymentRequest>
    findByAgent_IdAndIdempotencyKey(
            UUID agentId,
            String idempotencyKey
    );

    Optional<AgentPaymentRequest>
    findByAgent_IdAndRequestNonce(
            UUID agentId,
            String requestNonce
    );

    Optional<AgentPaymentRequest>
    findByReferenceAndMandate_Principal_Id(
            String reference,
            UUID principalId
    );

    List<AgentPaymentRequest>
    findAllByMandate_Principal_Id(UUID principalId);
}