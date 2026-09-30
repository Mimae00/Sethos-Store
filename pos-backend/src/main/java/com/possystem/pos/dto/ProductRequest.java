package com.possystem.pos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 40, message = "SKU must be at most 40 characters")
        String sku,

        @Size(max = 64, message = "Barcode must be at most 64 characters")
        String barcode,

        @NotBlank(message = "Product name is required")
        @Size(max = 160, message = "Product name must be at most 160 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Price allows at most 2 decimal places")
        BigDecimal price,

        @DecimalMin(value = "0.00", message = "Cost cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Cost allows at most 2 decimal places")
        BigDecimal cost,

        @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
        @DecimalMax(value = "100.00", message = "Tax rate cannot exceed 100")
        @Digits(integer = 3, fraction = 2, message = "Tax rate allows at most 2 decimal places")
        BigDecimal taxRate,

        @PositiveOrZero(message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @PositiveOrZero(message = "Reorder level cannot be negative")
        Integer reorderLevel,

        @Size(max = 16, message = "Unit must be at most 16 characters")
        String unit,

        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        Boolean active,

        Boolean trackStock,

        Long categoryId
) {
}
