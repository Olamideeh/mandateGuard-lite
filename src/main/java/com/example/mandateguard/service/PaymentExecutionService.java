package com.example.mandateguard.service;

import com.example.mandateguard.dto.PaymentRequestResponse;
import com.example.mandateguard.entity.AgentPaymentRequest;
import com.example.mandateguard.entity.PaymentMandate;
import com.example.mandateguard.enums.MandateStatus;
import com.example.mandateguard.enums.PaymentRequestStatus;
import com.example.mandateguard.exception.ResourceNotFoundException;
import com.example.mandateguard.repository.AgentPaymentRequestRepository;
import com.example.mandateguard.repository.PaymentMandateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.mandateguard.enums.AuditActorType;
import com.example.mandateguard.enums.AuditEventType;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentExecutionService {

    private final AuditService auditService;
    private final AgentPaymentRequestRepository requestRepository;
    private final PaymentMandateRepository mandateRepository;
    private final SimulatedPaymentProvider paymentProvider;

    @Transactional
    public PaymentRequestResponse execute(
            UUID principalId,
            UUID paymentRequestId
    ) {
        AgentPaymentRequest paymentRequest =
                requestRepository
                        .findByIdAndMandate_Principal_Id(
                                paymentRequestId,
                                principalId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment request not found"
                                )
                        );

        if (paymentRequest.getStatus() ==
                PaymentRequestStatus.EXECUTED) {
            return toResponse(paymentRequest);
        }

        if (paymentRequest.getStatus() ==
                PaymentRequestStatus.FAILED) {
            return toResponse(paymentRequest);
        }

        if (paymentRequest.getStatus() !=
                PaymentRequestStatus.AUTHORIZED) {
            throw new IllegalArgumentException(
                    "Only an authorized payment can be executed"
            );
        }

        ProviderPaymentResult providerResult =
                paymentProvider.process(paymentRequest);

        if (providerResult.successful()) {
            paymentRequest.setStatus(
                    PaymentRequestStatus.EXECUTED
            );
            paymentRequest.setProviderReference(
                    providerResult.providerReference()
            );
            paymentRequest.setExecutedAt(Instant.now());
            paymentRequest.setDecisionExplanation(
                    providerResult.message()
            );
        } else {
            paymentRequest.setStatus(
                    PaymentRequestStatus.FAILED
            );
            paymentRequest.setDecisionExplanation(
                    providerResult.message()
            );

            releaseReservedMandateUsage(
                    paymentRequest.getMandate(),
                    paymentRequest
            );
        }

        AgentPaymentRequest saved =
                requestRepository.save(paymentRequest);

        AuditEventType executionEventType =
                providerResult.successful()
                        ? AuditEventType.PAYMENT_EXECUTED
                        : AuditEventType.PAYMENT_FAILED;

        auditService.record(
                principalId,
                saved.getReference(),
                AuditActorType.SYSTEM,
                null,
                executionEventType,
                "PAYMENT_REQUEST",
                saved.getId().toString(),
                "AUTHORIZED",
                saved.getStatus().name(),
                providerResult.message() +
                        (
                                providerResult.providerReference() == null
                                        ? ""
                                        : " Provider reference: " +
                                        providerResult.providerReference()
                        )
        );

        return toResponse(saved);
    }

    private void releaseReservedMandateUsage(
            PaymentMandate mandate,
            AgentPaymentRequest paymentRequest
    ) {
        BigDecimal updatedConsumedAmount =
                mandate.getConsumedAmount()
                        .subtract(paymentRequest.getAmount());

        if (updatedConsumedAmount.compareTo(
                BigDecimal.ZERO
        ) < 0) {
            updatedConsumedAmount = BigDecimal.ZERO;
        }

        mandate.setConsumedAmount(updatedConsumedAmount);

        mandate.setUsedTransactionCount(
                Math.max(
                        0,
                        mandate.getUsedTransactionCount() - 1
                )
        );

        if (mandate.getStatus() ==
                MandateStatus.EXHAUSTED &&
                mandate.getExpiresAt().isAfter(Instant.now())) {
            mandate.setStatus(MandateStatus.ACTIVE);
        }

        mandateRepository.save(mandate);
    }

    private PaymentRequestResponse toResponse(
            AgentPaymentRequest request
    ) {
        return new PaymentRequestResponse(
                request.getId(),
                request.getReference(),
                request.getIdempotencyKey(),
                request.getRequestNonce(),
                request.getAmount(),
                request.getCurrencyCode(),
                request.getMerchantIdentifier(),
                request.getMerchantName(),
                request.getMerchantCategoryCode(),
                request.getMerchantCountryCode(),
                request.isMerchantVerified(),
                request.getDescription(),
                request.getStatus(),
                request.getDecision(),
                request.getReasonCode(),
                request.getDecisionExplanation(),
                request.getMandate().getId(),
                request.getAgent().getId(),
                request.getAgentRequestTimestamp(),
                request.getDecidedAt(),
                request.getApprovedAt(),
                request.getExecutedAt(),
                request.getProviderReference(),
                request.getCreatedAt()
        );
    }
}