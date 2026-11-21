package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "donation_camps", indexes = @Index(name = "idx_camp_date", columnList = "camp_date"))
@Getter
@Setter
@NoArgsConstructor
public class DonationCamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "camp_date", nullable = false)
    private LocalDate campDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 100)
    private String contactName;

    @Column(nullable = false, length = 15)
    private String contactPhone;

    /** Maximum number of bookings. */
    @Column(nullable = false)
    private int capacity = 50;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
