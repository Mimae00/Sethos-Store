package com.possystem.pos.dto;

import com.possystem.pos.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutRequest(
        @NotEmpty(message = "A sale needs at least one item")
        @Valid
        List<CheckoutItemRequest> items,

        Long customerId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        /** Required for cash sales so the change can be computed. */
        @DecimalMin(value = "0.00", message = "Amount tendered cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Amount tendered allows at most 2 decimal places")
        BigDecimal amountTendered,

        /** Discount on the whole basket, applied after tax. */
        @DecimalMin(value = "0.00", message = "Order discount cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Order discount allows at most 2 decimal places")
        BigDecimal orderDiscount,

        @Size(max = 120, message = "Cashier name must be at most 120 characters")
        String cashierName,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}
