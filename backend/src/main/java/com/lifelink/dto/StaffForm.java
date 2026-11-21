package com.lifelink.dto;

import com.lifelink.model.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** Admin creates staff (or other admin) accounts. */
@Getter
@Setter
public class StaffForm {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8-64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain letters and digits")
    private String password;

    @NotNull(message = "Select a role")
    private Role role = Role.STAFF;
}
