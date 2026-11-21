package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_camp", columnList = "camp_id"),
        @Index(name = "idx_appt_donor", columnList = "donor_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id")
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camp_id")
    private DonationCamp camp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    /** Short reference printed/encoded in the QR code. */
    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookedAt = LocalDateTime.now();
}
