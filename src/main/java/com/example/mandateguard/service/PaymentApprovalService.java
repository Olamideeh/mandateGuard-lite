package com.example.mandateguard.service;

import com.example.mandateguard.dto.ApprovalDecisionRequest;
import com.example.mandateguard.dto.PaymentApprovalResponse;
import com.example.mandateguard.entity.AgentPaymentRequest;
import com.example.mandateguard.entity.PaymentApproval;
import com.example.mandateguard.entity.PaymentMandate;
import com.example.mandateguard.enums.ApprovalStatus;
import com.example.mandateguard.enums.DecisionReasonCode;
import com.example.mandateguard.enums.MandateStatus;
import com.example.mandateguard.enums.PaymentDecision;
import com.example.mandateguard.enums.PaymentRequestStatus;
import com.example.mandateguard.exception.ResourceNotFoundException;
import com.example.mandateguard.repository.AgentPaymentRequestRepository;
import com.example.mandateguard.repository.PaymentApprovalRepository;
import com.example.mandateguard.repository.PaymentMandateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentApprovalService {

    private final PaymentApprovalRepository approvalRepository;
    private final AgentPaymentRequestRepository requestRepository;
    private final PaymentMandateRepository mandateRepository;
    private final MandateEvaluationService evaluationService;

    @Transactional
    public PaymentApprovalResponse approve(
            UUID principalId,
            UUID approvalId,
            ApprovalDecisionRequest decisionRequest
    ) {
        PaymentApproval approval = getOwnedApproval(
                principalId,
                approvalId
        );

        validatePendingApproval(approval);

        AgentPaymentRequest paymentRequest =
                approval.getPaymentRequest();

        PaymentMandate mandate =
                paymentRequest.getMandate();

        PaymentEvaluationResult currentEvaluation =
                evaluationService.evaluate(
                        paymentRequest.getAgent(),
                        mandate,
                        toEvaluationRequest(paymentRequest)
                );

        if (currentEvaluation.decision() ==
                PaymentDecision.DENIED) {
            throw new IllegalArgumentException(
                    "Payment no longer satisfies the mandate: " +
                            currentEvaluation.explanation()
            );
        }

        reserveMandateUsage(mandate, paymentRequest);

        Instant now = Instant.now();

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setDecisionNote(
                decisionRequest.decisionNote()
        );
        approval.setDecidedAt(now);

        paymentRequest.setDecision(PaymentDecision.ALLOWED);
        paymentRequest.setStatus(
                PaymentRequestStatus.AUTHORIZED
        );
        paymentRequest.setReasonCode(
                DecisionReasonCode.ALL_RULES_PASSED
        );
        paymentRequest.setDecisionExplanation(
                "The principal approved the payment"
        );
        paymentRequest.setApprovedAt(now);
        paymentRequest.setDecidedAt(now);

        approvalRepository.save(approval);
        requestRepository.save(paymentRequest);

        return toResponse(approval);
    }

    @Transactional
    public PaymentApprovalResponse reject(
            UUID principalId,
            UUID approvalId,
            ApprovalDecisionRequest decisionRequest
    ) {
        PaymentApproval approval = getOwnedApproval(
                principalId,
                approvalId
        );

        validatePendingApproval(approval);

        AgentPaymentRequest paymentRequest =
                approval.getPaymentRequest();

        Instant now = Instant.now();

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setDecisionNote(
                decisionRequest.decisionNote()
        );
        approval.setDecidedAt(now);

        paymentRequest.setDecision(PaymentDecision.DENIED);
        paymentRequest.setStatus(
                PaymentRequestStatus.REJECTED
        );
        paymentRequest.setReasonCode(
                DecisionReasonCode.PRINCIPAL_REJECTED
        );
        paymentRequest.setDecisionExplanation(
                "The principal rejected the payment"
        );
        paymentRequest.setDecidedAt(now);

        approvalRepository.save(approval);
        requestRepository.save(paymentRequest);

        return toResponse(approval);
    }

    @Transactional
    public List<PaymentApprovalResponse> getPendingApprovals(
            UUID principalId
    ) {
        List<PaymentApproval> approvals =
                approvalRepository
                        .findAllByPrincipal_IdAndStatusOrderByRequestedAtDesc(
                                principalId,
                                ApprovalStatus.PENDING
                        );

        Instant now = Instant.now();

        approvals.forEach(approval -> {
            if (!approval.getExpiresAt().isAfter(now)) {
                approval.setStatus(ApprovalStatus.EXPIRED);

                AgentPaymentRequest paymentRequest =
                        approval.getPaymentRequest();

                paymentRequest.setStatus(
                        PaymentRequestStatus.REJECTED
                );
                paymentRequest.setDecision(
                        PaymentDecision.DENIED
                );
                paymentRequest.setDecisionExplanation(
                        "The human approval request expired"
                );
                paymentRequest.setDecidedAt(now);
            }
        });

        approvalRepository.saveAll(approvals);

        return approvals.stream()
                .filter(approval ->
                        approval.getStatus() ==
                                ApprovalStatus.PENDING
                )
                .map(this::toResponse)
                .toList();
    }

    private PaymentApproval getOwnedApproval(
            UUID principalId,
            UUID approvalId
    ) {
        return approvalRepository
                .findByIdAndPrincipal_Id(
                        approvalId,
                        principalId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment approval not found"
                        )
                );
    }

    private void validatePendingApproval(
            PaymentApproval approval
    ) {
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only a pending approval can be decided"
            );
        }

        if (!approval.getExpiresAt().isAfter(Instant.now())) {
            approval.setStatus(ApprovalStatus.EXPIRED);

            throw new IllegalArgumentException(
                    "The payment approval has expired"
            );
        }
    }

    private void reserveMandateUsage(
            PaymentMandate mandate,
            AgentPaymentRequest paymentRequest
    ) {
        mandate.setConsumedAmount(
                mandate.getConsumedAmount()
                        .add(paymentRequest.getAmount())
        );

        mandate.setUsedTransactionCount(
                mandate.getUsedTransactionCount() + 1
        );

        if (mandate.getConsumedAmount()
                .compareTo(mandate.getTotalBudget()) >= 0 ||
                mandate.getUsedTransactionCount() >=
                        mandate.getMaximumTransactions()) {
            mandate.setStatus(MandateStatus.EXHAUSTED);
        }

        mandateRepository.save(mandate);
    }

    private com.example.mandateguard.dto.CreatePaymentRequest
    toEvaluationRequest(AgentPaymentRequest request) {
        return new com.example.mandateguard.dto.CreatePaymentRequest(
                request.getMandate().getId(),
                request.getReference(),
                request.getAmount(),
                request.getCurrencyCode(),
                request.getMerchantIdentifier(),
                request.getMerchantName(),
                request.getMerchantCategoryCode(),
                request.getMerchantCountryCode(),
                request.isMerchantVerified(),
                request.getDescription(),
                request.getAgentRequestTimestamp()
        );
    }

    private PaymentApprovalResponse toResponse(
            PaymentApproval approval
    ) {
        AgentPaymentRequest request =
                approval.getPaymentRequest();

        return new PaymentApprovalResponse(
                approval.getId(),
                request.getId(),
                request.getReference(),
                request.getAmount(),
                request.getCurrencyCode(),
                request.getMerchantName(),
                approval.getStatus(),
                approval.getDecisionNote(),
                approval.getRequestedAt(),
                approval.getExpiresAt(),
                approval.getDecidedAt()
        );
    }
}