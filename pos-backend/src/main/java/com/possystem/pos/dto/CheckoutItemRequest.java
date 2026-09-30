package com.possystem.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CheckoutItemRequest(
        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100000, message = "Quantity is unrealistically large")
        Integer quantity,

        /**
         * Manager override price. When null the product's current price is used, which is
         * the normal path; the terminal never sends a price it made up on its own.
         */
        @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Unit price allows at most 2 decimal places")
        BigDecimal unitPrice,

        /** Absolute discount on this line, not a percentage. */
        @DecimalMin(value = "0.00", message = "Discount cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Discount allows at most 2 decimal places")
        BigDecimal discountAmount
) {
}
