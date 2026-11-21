package com.lifelink.dto;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.ComponentType;
import com.lifelink.model.Urgency;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class BloodRequestForm {

    @NotBlank(message = "Patient name or reference is required")
    @Size(max = 100)
    private String patientReference;

    @NotNull(message = "Select the required blood group")
    private BloodGroup bloodGroup;

    @NotNull(message = "Select the component")
    private ComponentType component = ComponentType.WHOLE_BLOOD;

    @Min(value = 1, message = "At least 1 unit")
    @Max(value = 20, message = "Maximum 20 units per request")
    private int units = 1;

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150)
    private String hospital;

    @NotBlank(message = "City / location is required")
    @Size(max = 80)
    private String city;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String contactPhone;

    @NotNull(message = "Required date is needed")
    @FutureOrPresent(message = "Required date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate requiredDate;

    @NotNull(message = "Select urgency")
    private Urgency urgency = Urgency.NORMAL;
}
