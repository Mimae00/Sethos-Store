package com.possystem.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidSaleRequest(
        @NotBlank(message = "A reason is required when voiding a sale")
        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
}
