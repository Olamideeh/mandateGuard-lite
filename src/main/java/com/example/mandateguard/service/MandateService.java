package com.example.mandateguard.service;

import com.example.mandateguard.dto.CreateMandateRequest;
import com.example.mandateguard.dto.MandateResponse;
import com.example.mandateguard.entity.AiAgent;
import com.example.mandateguard.entity.PaymentMandate;
import com.example.mandateguard.entity.Principal;
import com.example.mandateguard.enums.AgentStatus;
import com.example.mandateguard.enums.MandateStatus;
import com.example.mandateguard.exception.ResourceNotFoundException;
import com.example.mandateguard.repository.AiAgentRepository;
import com.example.mandateguard.repository.PaymentMandateRepository;
import com.example.mandateguard.repository.PrincipalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MandateService {

    private final PaymentMandateRepository mandateRepository;
    private final PrincipalRepository principalRepository;
    private final AiAgentRepository agentRepository;
    private final MandateValidationService validationService;

    @Transactional
    public MandateResponse createMandate(
            UUID principalId,
            CreateMandateRequest request
    ) {
        validationService.validate(request);

        Principal principal = principalRepository.findById(principalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Principal not found")
                );

        if (!principal.isActive()) {
            throw new IllegalArgumentException(
                    "Inactive principal cannot create a mandate"
            );
        }

        AiAgent agent = agentRepository
                .findByIdAndPrincipal_Id(request.agentId(), principalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AI agent not found for this principal"
                        )
                );

        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only an active AI agent can receive a mandate"
            );
        }

        PaymentMandate mandate = new PaymentMandate();

        mandate.setMandateReference(generateReference());
        mandate.setName(request.name().trim());
        mandate.setStatus(MandateStatus.DRAFT);
        mandate.setCurrencyCode(request.currencyCode().toUpperCase());
        mandate.setMaximumSingleAmount(request.maxSingleAmount());
        mandate.setTotalBudget(request.totalBudget());
        mandate.setConsumedAmount(BigDecimal.ZERO);
        mandate.setApprovalThreshold(request.approvalThreshold());
        mandate.setMaximumTransactions(request.maxTransactions());
        mandate.setUsedTransactionCount(0);
        mandate.setAllowAnyVerifiedMerchant(
                request.allowAnyVerifiedMerchant()
        );

        mandate.setAllowedCountryCodes(
                normalizeUppercase(request.allowedCountryCodes())
        );

        mandate.setAllowedMerchantIdentifiers(
                normalize(request.allowedMerchantIds())
        );
        mandate.setAllowedMerchantCategoryCodes(
                normalizeUppercase(
                        request.allowedMerchantCategoryCodes()
                )
        );

        mandate.setPrincipal(principal);
        mandate.setAgent(agent);
        mandate.setValidFrom(request.validFrom());
        mandate.setExpiresAt(request.expiresAt());

        return toResponse(mandateRepository.save(mandate));
    }

    @Transactional
    public MandateResponse activateMandate(
            UUID principalId,
            UUID mandateId
    ) {
        PaymentMandate mandate = findOwnedMandate(
                principalId,
                mandateId
        );

        if (mandate.getStatus() != MandateStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Only a draft mandate can be activated"
            );
        }

        mandate.setStatus(MandateStatus.ACTIVE);

        return toResponse(mandateRepository.save(mandate));
    }

    @Transactional(readOnly = true)
    public List<MandateResponse> getMandates(UUID principalId) {
        return mandateRepository
                .findAllByPrincipal_IdOrderByCreatedAtDesc(principalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentMandate findOwnedMandate(
            UUID principalId,
            UUID mandateId
    ) {
        return mandateRepository
                .findByIdAndPrincipal_Id(mandateId, principalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mandate not found"
                        )
                );
    }

    private String generateReference() {
        return "MND-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private Set<String> normalize(Set<String> values) {
        if (values == null) {
            return new HashSet<>();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private Set<String> normalizeUppercase(Set<String> values) {
        return normalize(values)
                .stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private MandateResponse toResponse(PaymentMandate mandate) {
        BigDecimal remainingBudget = mandate.getTotalBudget()
                .subtract(mandate.getConsumedAmount());

        return new MandateResponse(
                mandate.getId(),
                mandate.getReference(),
                mandate.getName(),
                mandate.getStatus(),
                mandate.getCurrencyCode(),
                mandate.getMaximumSingleAmount(),
                mandate.getTotalBudget(),
                mandate.getConsumedAmount(),
                remainingBudget,
                mandate.getApprovalThreshold(),
                mandate.getMaximumTransactions(),
                mandate.getUsedTransactionCount(),
                mandate.isAllowAnyVerifiedMerchant(),
                mandate.getAllowedCountryCodes(),
                mandate.getAllowedMerchantIdentifiers(),
                mandate.getAllowedMerchantCategoryCodes(),
                mandate.getAgent().getId(),
                mandate.getAgent().getName(),
                mandate.getValidFrom(),
                mandate.getExpiresAt(),
                mandate.getCreatedAt(),
                mandate.getUpdatedAt()
        );
    }
    @Transactional
    public MandateResponse suspendMandate(
            UUID principalId,
            UUID mandateId
    ) {
        PaymentMandate mandate = findOwnedMandate(
                principalId,
                mandateId
        );

        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only an active mandate can be suspended"
            );
        }

        mandate.setStatus(MandateStatus.SUSPENDED);

        return toResponse(mandateRepository.save(mandate));
    }
    @Transactional
    public MandateResponse resumeMandate(
            UUID principalId,
            UUID mandateId
    ) {
        PaymentMandate mandate = findOwnedMandate(
                principalId,
                mandateId
        );

        if (mandate.getStatus() != MandateStatus.SUSPENDED) {
            throw new IllegalArgumentException(
                    "Only a suspended mandate can be resumed"
            );
        }

        if (!mandate.getExpiresAt().isAfter(java.time.Instant.now())) {
            mandate.setStatus(MandateStatus.EXPIRED);
            mandateRepository.save(mandate);

            throw new IllegalArgumentException(
                    "An expired mandate cannot be resumed"
            );
        }

        mandate.setStatus(MandateStatus.ACTIVE);

        return toResponse(mandateRepository.save(mandate));
    }
    @Transactional
    public MandateResponse revokeMandate(
            UUID principalId,
            UUID mandateId
    ) {
        PaymentMandate mandate = findOwnedMandate(
                principalId,
                mandateId
        );

        if (mandate.getStatus() == MandateStatus.REVOKED ||
                mandate.getStatus() == MandateStatus.EXPIRED ||
                mandate.getStatus() == MandateStatus.EXHAUSTED) {
            throw new IllegalArgumentException(
                    "A terminal mandate cannot be revoked again"
            );
        }

        mandate.setStatus(MandateStatus.REVOKED);

        return toResponse(mandateRepository.save(mandate));
    }
}