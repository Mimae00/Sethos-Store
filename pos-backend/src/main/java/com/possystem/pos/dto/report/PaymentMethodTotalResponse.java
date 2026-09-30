package com.possystem.pos.dto.report;

import com.possystem.pos.domain.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodTotalResponse(
        PaymentMethod paymentMethod,
        long saleCount,
        BigDecimal total
) {
}
