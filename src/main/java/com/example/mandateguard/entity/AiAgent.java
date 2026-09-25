package com.example.mandateguard.entity;

import com.example.mandateguard.enums.AgentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ai_agents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_principal_external_agent",
                        columnNames = {
                                "principal_id",
                                "external_agent_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_agent_key_fingerprint",
                        columnNames = "public_key_fingerprint"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "external_agent_id",
            nullable = false,
            length = 100
    )
    private String externalAgentId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String provider;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status = AgentStatus.PENDING;

    @Column(
            name = "public_key_pem",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String publicKeyPem;

    @Column(
            name = "public_key_fingerprint",
            nullable = false,
            length = 64
    )
    private String publicKeyFingerprint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "principal_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_agent_principal"
            )
    )
    private Principal principal;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant lastAuthenticatedAt;

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();

        if (status == null) {
            status = AgentStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();
    }
}