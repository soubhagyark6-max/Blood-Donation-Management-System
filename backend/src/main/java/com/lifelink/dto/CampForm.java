package com.lifelink.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CampForm {

    @NotBlank(message = "Camp name is required")
    @Size(max = 150)
    private String name;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Camp date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate campDate;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    @NotBlank(message = "Venue is required")
    @Size(max = 200)
    private String venue;

    @NotBlank(message = "City is required")
    @Size(max = 80)
    private String city;

    @NotBlank(message = "Contact person is required")
    @Size(max = 100)
    private String contactName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String contactPhone;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 2000)
    private int capacity = 50;

    @Size(max = 500)
    private String description;
}
