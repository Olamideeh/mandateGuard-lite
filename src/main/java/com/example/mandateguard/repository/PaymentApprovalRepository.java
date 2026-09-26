package com.example.mandateguard.repository;

import com.example.mandateguard.entity.PaymentApproval;
import com.example.mandateguard.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentApprovalRepository
        extends JpaRepository<PaymentApproval, UUID> {

    Optional<PaymentApproval> findByIdAndPrincipal_Id(
            UUID approvalId,
            UUID principalId
    );

    Optional<PaymentApproval> findByPaymentRequest_Id(
            UUID paymentRequestId
    );

    List<PaymentApproval>
    findAllByPrincipal_IdAndStatusOrderByRequestedAtDesc(
            UUID principalId,
            ApprovalStatus status
    );
}