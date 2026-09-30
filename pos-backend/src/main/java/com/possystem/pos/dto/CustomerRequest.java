package com.possystem.pos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 160, message = "Customer name must be at most 160 characters")
        String name,

        @Size(max = 32, message = "Phone must be at most 32 characters")
        String phone,

        @Email(message = "Email must be a valid address")
        @Size(max = 160, message = "Email must be at most 160 characters")
        String email,

        @Size(max = 320, message = "Address must be at most 320 characters")
        String address,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes
) {
}
