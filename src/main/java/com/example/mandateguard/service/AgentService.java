package com.example.mandateguard.service;

import com.example.mandateguard.dto.*;
import com.example.mandateguard.entity.AiAgent;
import com.example.mandateguard.entity.Principal;
import com.example.mandateguard.enums.AgentStatus;
import com.example.mandateguard.exception.*;
import com.example.mandateguard.repository.*;
import com.example.mandateguard.security.AgentKeyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private final AiAgentRepository agentRepository;
    private final PrincipalRepository principalRepository;
    private final AgentKeyService agentKeyService;

    public AgentService(
            AiAgentRepository agentRepository,
            PrincipalRepository principalRepository,
            AgentKeyService agentKeyService
    ) {
        this.agentRepository = agentRepository;
        this.principalRepository = principalRepository;
        this.agentKeyService = agentKeyService;
    }

    @Transactional
    public AgentResponse registerAgent(
            UUID principalId,
            RegisterAgentRequest request
    ) {
        Principal principal = principalRepository
                .findById(principalId)
                .filter(Principal::isActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active principal not found"
                        )
                );

        String externalAgentId =
                request.externalAgentId().trim();

        if (agentRepository
                .existsByPrincipal_IdAndExternalAgentId(
                        principalId,
                        externalAgentId
                )) {
            throw new DuplicateResourceException(
                    "This external agent ID is already registered"
            );
        }

        AgentKeyService.ValidatedPublicKey validatedKey =
                agentKeyService.validateAndFingerprint(
                        request.publicKeyPem()
                );

        if (agentRepository
                .findByPublicKeyFingerprint(
                        validatedKey.fingerprint()
                )
                .isPresent()) {
            throw new DuplicateResourceException(
                    "This public key is already registered"
            );
        }

        AiAgent agent = AiAgent.builder()
                .externalAgentId(externalAgentId)
                .name(request.name().trim())
                .provider(request.provider().trim())
                .status(AgentStatus.PENDING)
                .publicKeyPem(
                        validatedKey.normalizedPem()
                )
                .publicKeyFingerprint(
                        validatedKey.fingerprint()
                )
                .principal(principal)
                .build();

        return mapToResponse(
                agentRepository.save(agent)
        );
    }

    @Transactional
    public AgentResponse activateAgent(
            UUID principalId,
            UUID agentId
    ) {
        AiAgent agent = findOwnedAgent(
                principalId,
                agentId
        );

        if (agent.getStatus() != AgentStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only a PENDING agent can be activated"
            );
        }

        agent.setStatus(AgentStatus.ACTIVE);

        return mapToResponse(
                agentRepository.save(agent)
        );
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> getAgents(
            UUID principalId
    ) {
        return agentRepository
                .findAllByPrincipal_Id(principalId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AiAgent findOwnedAgent(
            UUID principalId,
            UUID agentId
    ) {
        return agentRepository
                .findByIdAndPrincipal_Id(
                        agentId,
                        principalId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AI agent not found"
                        )
                );
    }

    private AgentResponse mapToResponse(
            AiAgent agent
    ) {
        return new AgentResponse(
                agent.getId(),
                agent.getExternalAgentId(),
                agent.getName(),
                agent.getProvider(),
                agent.getStatus(),
                agent.getPublicKeyFingerprint(),
                agent.getPrincipal().getId(),
                agent.getCreatedAt()
        );
    }
}