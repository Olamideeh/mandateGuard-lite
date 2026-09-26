package com.example.mandateguard.service;

import com.example.mandateguard.dto.CreatePaymentRequest;
import com.example.mandateguard.dto.PaymentRequestResponse;
import com.example.mandateguard.entity.AgentPaymentRequest;
import com.example.mandateguard.entity.AiAgent;
import com.example.mandateguard.entity.PaymentMandate;
import com.example.mandateguard.enums.AgentStatus;
import com.example.mandateguard.enums.DecisionReasonCode;
import com.example.mandateguard.enums.MandateStatus;
import com.example.mandateguard.enums.PaymentDecision;
import com.example.mandateguard.exception.IdempotencyConflictException;
import com.example.mandateguard.exception.ResourceNotFoundException;
import com.example.mandateguard.repository.AgentPaymentRequestRepository;
import com.example.mandateguard.repository.AiAgentRepository;
import com.example.mandateguard.repository.PaymentMandateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.mandateguard.entity.PaymentApproval;
import com.example.mandateguard.enums.ApprovalStatus;
import com.example.mandateguard.repository.PaymentApprovalRepository;
import com.example.mandateguard.enums.AuditActorType;
import com.example.mandateguard.enums.AuditEventType;


import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentRequestService {

    private static final Duration ALLOWED_CLOCK_DIFFERENCE =
            Duration.ofMinutes(5);

    private final AuditService auditService;
    private final PaymentApprovalRepository approvalRepository;
    private final AgentPaymentRequestRepository requestRepository;
    private final AiAgentRepository agentRepository;
    private final PaymentMandateRepository mandateRepository;
    private final AgentSignatureService signatureService;
    private final MandateEvaluationService evaluationService;

    @Transactional
    public PaymentRequestResponse submitPayment(
            UUID agentId,
            String idempotencyKey,
            String nonce,
            String signature,
            CreatePaymentRequest request
    ) {
        validateHeaders(idempotencyKey, nonce, signature);

        AiAgent agent = agentRepository.findById(agentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AI agent not found"
                        )
                );

        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "AI agent is not active"
            );
        }
        UUID principalId = agent.getPrincipal().getId();

        auditService.record(
                principalId,
                request.paymentReference(),
                AuditActorType.AI_AGENT,
                agentId.toString(),
                AuditEventType.PAYMENT_REQUEST_RECEIVED,
                "PAYMENT_REQUEST",
                request.paymentReference(),
                null,
                "RECEIVED",
                "Signed payment request received from AI agent"
        );

        String payloadHash;

        try {
            payloadHash = signatureService.verifyAndHash(
                    agentId,
                    idempotencyKey.trim(),
                    nonce.trim(),
                    signature.trim(),
                    agent.getPublicKeyPem(),
                    request
            );

            auditService.record(
                    principalId,
                    request.paymentReference(),
                    AuditActorType.SYSTEM,
                    null,
                    AuditEventType.SIGNATURE_VERIFIED,
                    "PAYMENT_REQUEST",
                    request.paymentReference(),
                    "RECEIVED",
                    "SIGNATURE_VERIFIED",
                    "The AI agent RSA signature was verified"
            );

        } catch (IllegalArgumentException exception) {

            auditService.record(
                    principalId,
                    request.paymentReference(),
                    AuditActorType.SYSTEM,
                    null,
                    AuditEventType.SIGNATURE_REJECTED,
                    "PAYMENT_REQUEST",
                    request.paymentReference(),
                    "RECEIVED",
                    "SIGNATURE_REJECTED",
                    exception.getMessage()
            );

            throw exception;
        }

        validateTimestamp(request.agentTimestamp());

        if (requestRepository.existsByAgent_IdAndRequestNonce(
                agentId,
                nonce.trim()
        )) {
            throw new IdempotencyConflictException(
                    "The request nonce has already been used"
            );
        }

        if (requestRepository.existsByReference(
                request.paymentReference().trim()
        )) {
            throw new IdempotencyConflictException(
                    "The payment reference already exists"
            );
        }

        PaymentMandate mandate = mandateRepository
                .findById(request.mandateId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment mandate not found"
                        )
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        AgentPaymentRequest paymentRequest =
                new AgentPaymentRequest();

        paymentRequest.setReference(
                request.paymentReference().trim()
        );
        paymentRequest.setIdempotencyKey(
                idempotencyKey.trim()
        );
        paymentRequest.setRequestNonce(nonce.trim());
        paymentRequest.setAmount(request.amount());
        paymentRequest.setCurrencyCode(
                request.currencyCode().toUpperCase()
        );
        paymentRequest.setMerchantIdentifier(
                request.merchantIdentifier().trim()
        );
        paymentRequest.setMerchantName(
                request.merchantName().trim()
        );
        paymentRequest.setMerchantCountryCode(
                request.merchantCountryCode().toUpperCase()
        );
        paymentRequest.setMerchantCategoryCode(
                request.merchantCategoryCode()
        );
        paymentRequest.setMerchantVerified(
                request.merchantVerified()
        );
        paymentRequest.setDescription(request.description());
        paymentRequest.setStatus(result.status());
        paymentRequest.setDecision(result.decision());
        paymentRequest.setReasonCode(result.reasonCode());
        paymentRequest.setDecisionExplanation(
                result.explanation()
        );
        paymentRequest.setRequestSignature(signature.trim());
        paymentRequest.setRequestPayloadHash(payloadHash);
        paymentRequest.setAgentRequestTimestamp(
                request.agentTimestamp()
        );
        paymentRequest.setAgent(agent);
        paymentRequest.setMandate(mandate);
        paymentRequest.setDecidedAt(Instant.now());

        if (result.decision() == PaymentDecision.ALLOWED) {
            reserveMandateUsage(mandate, request);
        }

        updateTerminalMandateStatus(mandate, result);

        AgentPaymentRequest savedRequest =
                requestRepository.save(paymentRequest);
        if (result.decision() ==
                PaymentDecision.REQUIRES_APPROVAL) {

            createApproval(savedRequest, mandate);
        }

        AuditEventType decisionEventType =
                switch (result.decision()) {
                    case ALLOWED ->
                            AuditEventType.PAYMENT_ALLOWED;
                    case DENIED ->
                            AuditEventType.PAYMENT_DENIED;
                    case REQUIRES_APPROVAL ->
                            AuditEventType.APPROVAL_REQUESTED;
                };

        auditService.record(
                principalId,
                savedRequest.getReference(),
                AuditActorType.SYSTEM,
                null,
                decisionEventType,
                "PAYMENT_REQUEST",
                savedRequest.getId().toString(),
                "RECEIVED",
                savedRequest.getStatus().name(),
                result.reasonCode().name() +
                        ": " +
                        result.explanation()
        );

        return toResponse(savedRequest);
    }

    private void createApproval(
            AgentPaymentRequest paymentRequest,
            PaymentMandate mandate
    ) {
        PaymentApproval approval = new PaymentApproval();

        approval.setPaymentRequest(paymentRequest);
        approval.setPrincipal(mandate.getPrincipal());
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setExpiresAt(
                Instant.now().plus(Duration.ofMinutes(15))
        );

        approvalRepository.save(approval);
    }

    private void reserveMandateUsage(
            PaymentMandate mandate,
            CreatePaymentRequest request
    ) {
        mandate.setConsumedAmount(
                mandate.getConsumedAmount()
                        .add(request.amount())
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

    private void updateTerminalMandateStatus(
            PaymentMandate mandate,
            PaymentEvaluationResult result
    ) {
        if (result.reasonCode() ==
                DecisionReasonCode.MANDATE_EXPIRED) {
            mandate.setStatus(MandateStatus.EXPIRED);
            mandateRepository.save(mandate);
        }

        if (result.reasonCode() ==
                DecisionReasonCode.MANDATE_EXHAUSTED ||
                result.reasonCode() ==
                        DecisionReasonCode.TRANSACTION_LIMIT_EXCEEDED ||
                result.reasonCode() ==
                        DecisionReasonCode.TOTAL_BUDGET_EXCEEDED) {

            boolean noBudgetRemaining =
                    mandate.getConsumedAmount()
                            .compareTo(
                                    mandate.getTotalBudget()
                            ) >= 0;

            boolean noTransactionsRemaining =
                    mandate.getUsedTransactionCount() >=
                            mandate.getMaximumTransactions();

            if (noBudgetRemaining || noTransactionsRemaining) {
                mandate.setStatus(MandateStatus.EXHAUSTED);
                mandateRepository.save(mandate);
            }
        }
    }

    private void validateHeaders(
            String idempotencyKey,
            String nonce,
            String signature
    ) {
        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException(
                    "X-Nonce header is required"
            );
        }

        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException(
                    "X-Signature header is required"
            );
        }
    }

    private void validateTimestamp(Instant agentTimestamp) {
        Duration difference = Duration.between(
                agentTimestamp,
                Instant.now()
        ).abs();

        if (difference.compareTo(
                ALLOWED_CLOCK_DIFFERENCE
        ) > 0) {
            throw new IllegalArgumentException(
                    "Agent timestamp must be within 5 minutes of server time"
            );
        }
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
    @Transactional(readOnly = true)
    public java.util.List<PaymentRequestResponse> getPayments(
            UUID principalId
    ) {
        return requestRepository
                .findAllByMandate_Principal_IdOrderByCreatedAtDesc(
                        principalId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }
}