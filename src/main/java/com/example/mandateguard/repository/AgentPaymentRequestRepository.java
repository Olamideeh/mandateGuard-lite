package com.example.mandateguard.repository;

import com.example.mandateguard.entity.AgentPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentPaymentRequestRepository
        extends JpaRepository<AgentPaymentRequest, UUID> {

    Optional<AgentPaymentRequest>
    findByAgent_IdAndIdempotencyKey(
            UUID agentId,
            String idempotencyKey
    );

    boolean existsByAgent_IdAndRequestNonce(
            UUID agentId,
            String requestNonce
    );

    boolean existsByReference(String reference);

    List<AgentPaymentRequest>
    findAllByMandate_Principal_IdOrderByCreatedAtDesc(
            UUID principalId
    );
    Optional<AgentPaymentRequest>
    findByIdAndMandate_Principal_Id(
            UUID paymentRequestId,
            UUID principalId
    );
}