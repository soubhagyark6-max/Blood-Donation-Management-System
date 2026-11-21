package com.lifelink.dto;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.ComponentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Used for both receiving and issuing stock. */
@Getter
@Setter
public class StockForm {

    @NotNull(message = "Select blood group")
    private BloodGroup bloodGroup;

    @NotNull(message = "Select component")
    private ComponentType component = ComponentType.WHOLE_BLOOD;

    @Min(value = 1, message = "At least 1 unit")
    @Max(value = 500, message = "Too many units in one entry")
    private int units = 1;

    @PastOrPresent(message = "Collection date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate collectionDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @Size(max = 300)
    private String note;
}
