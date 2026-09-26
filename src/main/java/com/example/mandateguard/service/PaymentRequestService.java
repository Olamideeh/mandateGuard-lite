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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentRequestService {

    private static final Duration ALLOWED_CLOCK_DIFFERENCE =
            Duration.ofMinutes(5);

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

        String payloadHash = signatureService.verifyAndHash(
                agentId,
                idempotencyKey.trim(),
                nonce.trim(),
                signature.trim(),
                agent.getPublicKeyPem(),
                request
        );

        var existingRequest = requestRepository
                .findByAgent_IdAndIdempotencyKey(
                        agentId,
                        idempotencyKey.trim()
                );

        if (existingRequest.isPresent()) {
            AgentPaymentRequest existing = existingRequest.get();

            if (!existing.getRequestPayloadHash()
                    .equals(payloadHash)) {
                throw new IdempotencyConflictException(
                        "The idempotency key was already used for a different payment request"
                );
            }

            return toResponse(existing);
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

        return toResponse(savedRequest);
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
}