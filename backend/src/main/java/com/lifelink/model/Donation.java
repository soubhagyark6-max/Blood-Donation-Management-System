package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations", indexes = @Index(name = "idx_donation_date", columnList = "donation_date"))
@Getter
@Setter
@NoArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id")
    private Donor donor;

    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentType component = ComponentType.WHOLE_BLOOD;

    @Column(nullable = false)
    private int units = 1;

    @Column(length = 150)
    private String location;

    @Column(length = 300)
    private String notes;

    /** True when blood bank staff recorded it; false when the donor reported it themselves. */
    @Column(nullable = false)
    private boolean staffVerified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
