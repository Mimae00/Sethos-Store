package com.possystem.pos.dto;

import com.possystem.pos.domain.PaymentMethod;
import com.possystem.pos.domain.Sale;
import com.possystem.pos.domain.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * History row. Deliberately excludes line items so the list endpoint stays a single query.
 */
public record SaleSummaryResponse(
        Long id,
        String reference,
        Instant soldAt,
        SaleStatus status,
        PaymentMethod paymentMethod,
        BigDecimal total,
        BigDecimal taxTotal,
        BigDecimal discountTotal,
        String cashierName,
        String customerName
) {

    public static SaleSummaryResponse from(Sale sale) {
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getReference(),
                sale.getSoldAt(),
                sale.getStatus(),
                sale.getPaymentMethod(),
                sale.getTotal(),
                sale.getTaxTotal(),
                sale.getDiscountTotal(),
                sale.getCashierName(),
                sale.getCustomer() == null ? null : sale.getCustomer().getName()
        );
    }
}
