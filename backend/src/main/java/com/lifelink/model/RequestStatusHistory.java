package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_status_history", indexes = @Index(name = "idx_hist_request", columnList = "request_id"))
@Getter
@Setter
@NoArgsConstructor
public class RequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private BloodRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(length = 500)
    private String remarks;

    @Column(length = 100)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
