package com.possystem.pos.dto;

import com.possystem.pos.domain.StockMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        /** Signed delta. Negative values reduce stock. */
        @NotNull(message = "Quantity change is required")
        Integer quantityChange,

        @NotNull(message = "Movement type is required")
        StockMovementType type,

        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
}
