package com.example.mandateguard.entity;

import com.example.mandateguard.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_approvals",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_approval_payment_request",
                        columnNames = "payment_request_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_request_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_approval_payment_request"
            )
    )
    private AgentPaymentRequest paymentRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "principal_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_approval_principal"
            )
    )
    private Principal principal;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(length = 1000)
    private String decisionNote;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant decidedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();

        if (status == null) {
            status = ApprovalStatus.PENDING;
        }

        requestedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();
    }
}