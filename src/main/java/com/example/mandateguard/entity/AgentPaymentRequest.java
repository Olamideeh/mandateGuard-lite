package com.example.mandateguard.entity;

import com.example.mandateguard.enums.DecisionReasonCode;
import com.example.mandateguard.enums.PaymentDecision;
import com.example.mandateguard.enums.PaymentRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "agent_payment_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_request_reference",
                        columnNames = "reference"
                ),
                @UniqueConstraint(
                        name = "uk_agent_idempotency_key",
                        columnNames = {
                                "agent_id",
                                "idempotency_key"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_agent_request_nonce",
                        columnNames = {
                                "agent_id",
                                "request_nonce"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentPaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String reference;

    @Column(
            name = "idempotency_key",
            nullable = false,
            length = 100
    )
    private String idempotencyKey;

    @Column(
            name = "request_nonce",
            nullable = false,
            length = 100
    )
    private String requestNonce;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(
            name = "currency_code",
            nullable = false,
            length = 3
    )
    private String currencyCode;

    @Column(
            name = "merchant_identifier",
            nullable = false,
            length = 150
    )
    private String merchantIdentifier;

    @Column(
            name = "merchant_name",
            nullable = false,
            length = 150
    )
    private String merchantName;

    @Column(
            name = "merchant_country_code",
            nullable = false,
            length = 2
    )
    private String merchantCountryCode;

    @Column(
            name = "merchant_category_code",
            nullable = false,
            length = 4
    )
    private String merchantCategoryCode;

    @Builder.Default
    @Column(
            name = "merchant_verified",
            nullable = false
    )
    private boolean merchantVerified = false;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentRequestStatus status =
            PaymentRequestStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50)
    private DecisionReasonCode reasonCode;

    @Column(
            name = "decision_explanation",
            length = 1000
    )
    private String decisionExplanation;

    @Column(
            name = "request_signature",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String requestSignature;

    @Column(
            name = "request_payload_hash",
            nullable = false,
            length = 64
    )
    private String requestPayloadHash;

    @Column(
            name = "agent_request_timestamp",
            nullable = false
    )
    private Instant agentRequestTimestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "agent_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_request_agent"
            )
    )
    private AiAgent agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "mandate_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_request_mandate"
            )
    )
    private PaymentMandate mandate;

    private Instant decidedAt;

    private Instant approvedAt;

    private Instant executedAt;

    @Column(
            name = "provider_reference",
            length = 150
    )
    private String providerReference;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();

        if (status == null) {
            status = PaymentRequestStatus.RECEIVED;
        }

        if (currencyCode != null) {
            currencyCode = currencyCode.toUpperCase();
        }

        if (merchantCountryCode != null) {
            merchantCountryCode =
                    merchantCountryCode.toUpperCase();
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();
    }
}