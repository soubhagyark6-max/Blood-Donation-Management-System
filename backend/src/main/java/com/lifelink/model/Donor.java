package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "donors", indexes = {
        @Index(name = "idx_donor_group", columnList = "blood_group"),
        @Index(name = "idx_donor_city", columnList = "city")
})
@Getter
@Setter
@NoArgsConstructor
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 10)
    private BloodGroup bloodGroup;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(length = 300)
    private String address;

    /** Optional - as reported by the donor or recorded by staff. */
    private LocalDate lastDonationDate;

    /** Donor says they are willing to be contacted for donation. */
    @Column(nullable = false)
    private boolean available = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactPreference contactPreference = ContactPreference.PHONE_AND_EMAIL;

    /** Set by blood bank staff after checking the donor's details. This is NOT a medical clearance. */
    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    public int getAge() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
