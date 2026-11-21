package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_requests", indexes = {
        @Index(name = "idx_req_status", columnList = "status"),
        @Index(name = "idx_req_group", columnList = "blood_group"),
        @Index(name = "idx_req_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    /** Patient name or a reference code - the requester decides how much to disclose. */
    @Column(nullable = false, length = 100)
    private String patientReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 10)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentType component = ComponentType.WHOLE_BLOOD;

    @Column(nullable = false)
    private int units;

    @Column(nullable = false, length = 150)
    private String hospital;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 15)
    private String contactPhone;

    @Column(nullable = false)
    private LocalDate requiredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Urgency urgency = Urgency.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    /** Latest staff remark (full history is in RequestStatusHistory). */
    @Column(length = 500)
    private String staffRemarks;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
