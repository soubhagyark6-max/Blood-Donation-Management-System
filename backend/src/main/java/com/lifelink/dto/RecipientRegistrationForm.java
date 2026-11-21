package com.lifelink.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecipientRegistrationForm {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Name must be 3-100 characters")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Name may contain letters, spaces, . ' - only")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8-64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain letters and digits")
    private String password;

    @NotBlank(message = "Please confirm the password")
    private String confirmPassword;
}
