package com.example.mandateguard.repository;

import com.example.mandateguard.entity.PaymentMandate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMandateRepository
        extends JpaRepository<PaymentMandate, UUID> {

    boolean existsByReference(String reference);

    Optional<PaymentMandate>
    findByReferenceAndPrincipal_Id(
            String reference,
            UUID principalId
    );

    Optional<PaymentMandate>
    findByIdAndPrincipal_Id(
            UUID mandateId,
            UUID principalId
    );

    List<PaymentMandate>
    findAllByPrincipal_Id(UUID principalId);

    List<PaymentMandate>
    findAllByAgent_Id(UUID agentId);
}