package com.example.mandateguard.entity;

import com.example.mandateguard.enums.PrincipalType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "principals",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_principal_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Principal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrincipalType type;

    @Column(
            name = "country_code",
            nullable = false,
            length = 2
    )
    private String countryCode;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (countryCode != null) {
            countryCode = countryCode.toUpperCase();
        }

        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();

        if (countryCode != null) {
            countryCode = countryCode.toUpperCase();
        }

        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}