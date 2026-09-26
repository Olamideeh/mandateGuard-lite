package com.example.mandateguard.entity;

import com.example.mandateguard.enums.MandateStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "payment_mandates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mandate_reference",
                        columnNames = "reference"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMandate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String reference;

    @Column(nullable = false, length = 150)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MandateStatus status = MandateStatus.DRAFT;

    @Column(
            name = "currency_code",
            nullable = false,
            length = 3
    )
    private String currencyCode;

    @Column(
            name = "maximum_single_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal maximumSingleAmount;

    @Column(
            name = "total_budget",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal totalBudget;

    @Builder.Default
    @Column(
            name = "consumed_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal consumedAmount = BigDecimal.ZERO;

    @Column(
            name = "approval_threshold",
            precision = 19,
            scale = 2
    )
    private BigDecimal approvalThreshold;

    @Column(
            name = "maximum_transactions",
            nullable = false
    )
    private int maximumTransactions;

    @Builder.Default
    @Column(
            name = "used_transaction_count",
            nullable = false
    )
    private int usedTransactionCount = 0;

    @Builder.Default
    @Column(
            name = "allow_any_verified_merchant",
            nullable = false
    )
    private boolean allowAnyVerifiedMerchant = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "mandate_allowed_countries",
            joinColumns = @JoinColumn(name = "mandate_id")
    )
    @Column(
            name = "country_code",
            nullable = false,
            length = 2
    )
    @Builder.Default
    private Set<String> allowedCountryCodes = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "mandate_allowed_merchants",
            joinColumns = @JoinColumn(name = "mandate_id")
    )
    @Column(
            name = "merchant_identifier",
            nullable = false,
            length = 150
    )
    @Builder.Default
    private Set<String> allowedMerchantIdentifiers =
            new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "mandate_allowed_categories",
            joinColumns = @JoinColumn(name = "mandate_id")
    )
    @Column(
            name = "merchant_category_code",
            nullable = false,
            length = 4
    )
    @Builder.Default
    private Set<String> allowedMerchantCategoryCodes =
            new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "principal_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_mandate_principal"
            )
    )
    private Principal principal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "agent_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_mandate_agent"
            )
    )
    private AiAgent agent;

    @Column(nullable = false)
    private Instant validFrom;

    @Column(nullable = false)
    private Instant expiresAt;

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
            status = MandateStatus.DRAFT;
        }

        if (consumedAmount == null) {
            consumedAmount = BigDecimal.ZERO;
        }

        if (currencyCode != null) {
            currencyCode = currencyCode.toUpperCase();
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();

        if (currencyCode != null) {
            currencyCode = currencyCode.toUpperCase();
        }
    }

    public void setMandateReference(String s) {

    }
}