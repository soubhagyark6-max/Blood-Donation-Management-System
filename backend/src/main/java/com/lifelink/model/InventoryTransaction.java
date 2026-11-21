package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Immutable ledger of every stock movement. */
@Entity
@Table(name = "inventory_transactions", indexes = @Index(name = "idx_tx_created", columnList = "created_at"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TxType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentType component;

    @Column(nullable = false)
    private int units;

    @Column(length = 30)
    private String batchCode;

    /** Related blood request id when stock was issued for a request. */
    private Long requestId;

    @Column(length = 300)
    private String note;

    @Column(length = 100)
    private String performedBy;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
