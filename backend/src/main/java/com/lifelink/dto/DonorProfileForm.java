package com.lifelink.dto;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.ContactPreference;
import com.lifelink.model.Gender;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Donor details - used for registration (extended) and for profile editing. */
@Getter
@Setter
public class DonorProfileForm {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Name must be 3-100 characters")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Name may contain letters, spaces, . ' - only")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @NotNull(message = "Please select gender")
    private Gender gender;

    @NotNull(message = "Please select blood group")
    private BloodGroup bloodGroup;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String phone;

    @NotBlank(message = "City is required")
    @Size(max = 80)
    private String city;

    @Size(max = 300, message = "Address is too long")
    private String address;

    @PastOrPresent(message = "Last donation date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastDonationDate;

    private boolean available = true;

    @NotNull(message = "Choose a contact preference")
    private ContactPreference contactPreference = ContactPreference.PHONE_AND_EMAIL;
}
