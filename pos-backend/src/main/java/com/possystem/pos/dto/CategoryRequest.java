package com.possystem.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 80, message = "Category name must be at most 80 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @Size(max = 16, message = "Colour must be at most 16 characters")
        String color,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder
) {
}
