package com.example.mandateguard.repository;

import com.example.mandateguard.entity.PaymentMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select mandate
        from PaymentMandate mandate
        where mandate.id = :mandateId
        """)
    Optional<PaymentMandate> findByIdForUpdate(
            @Param("mandateId") UUID mandateId
    );
}