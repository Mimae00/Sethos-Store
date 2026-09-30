package com.possystem.pos.repository.projection;

import com.possystem.pos.domain.PaymentMethod;

import java.math.BigDecimal;

/**
 * Takings split by tender type, used for the end-of-day cash count.
 */
public record PaymentMethodTotalRow(
        PaymentMethod paymentMethod,
        Long saleCount,
        BigDecimal total
) {
}
