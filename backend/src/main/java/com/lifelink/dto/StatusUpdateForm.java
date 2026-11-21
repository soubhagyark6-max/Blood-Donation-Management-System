package com.lifelink.dto;

import com.lifelink.model.RequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateForm {

    @NotNull(message = "Select the new status")
    private RequestStatus status;

    @Size(max = 500, message = "Remarks are too long")
    private String remarks;
}
