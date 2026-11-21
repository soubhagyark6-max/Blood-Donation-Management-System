package com.lifelink.dto;

import com.lifelink.model.ComponentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class DonationForm {

    @NotNull(message = "Donation date is required")
    @PastOrPresent(message = "Donation date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate donationDate;

    @NotNull
    private ComponentType component = ComponentType.WHOLE_BLOOD;

    @Min(1)
    @Max(4)
    private int units = 1;

    @Size(max = 150)
    private String location;

    @Size(max = 300)
    private String notes;

    /** Staff only: also add the donated units to inventory. */
    private boolean addToInventory;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;
}
