package com.example.mandateguard.repository;

import com.example.mandateguard.entity.PaymentMandate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMandateRepository
        extends JpaRepository<PaymentMandate, UUID> {

    Optional<PaymentMandate> findByIdAndPrincipal_Id(
            UUID mandateId,
            UUID principalId
    );

    List<PaymentMandate> findAllByPrincipal_IdOrderByCreatedAtDesc(
            UUID principalId
    );
}