package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One stock batch (lot) of a single blood group + component.
 * Different components are different rows and are never merged or substituted.
 */
@Entity
@Table(name = "blood_inventory", indexes = {
        @Index(name = "idx_inv_group_comp", columnList = "blood_group,component"),
        @Index(name = "idx_inv_expiry", columnList = "expiry_date")
})
@Getter
@Setter
@NoArgsConstructor
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String batchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 10)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentType component;

    /** Units still available in this batch. Never negative. */
    @Column(nullable = false)
    private int units;

    /** Units originally received in this batch. */
    @Column(nullable = false)
    private int unitsReceived;

    @Column(nullable = false)
    private LocalDate collectionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Version
    private Long version;

    @Transient
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
}
