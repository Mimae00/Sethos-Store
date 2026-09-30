package com.possystem.pos.dto;

import com.possystem.pos.domain.PaymentMethod;
import com.possystem.pos.domain.Sale;
import com.possystem.pos.domain.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full receipt payload, including every line.
 */
public record SaleResponse(
        Long id,
        String reference,
        Instant soldAt,
        SaleStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal orderDiscount,
        BigDecimal taxTotal,
        BigDecimal total,
        BigDecimal amountTendered,
        BigDecimal changeDue,
        String cashierName,
        String note,
        String voidReason,
        Instant voidedAt,
        CustomerResponse customer,
        int totalUnits,
        List<SaleItemResponse> items
) {

    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getReference(),
                sale.getSoldAt(),
                sale.getStatus(),
                sale.getPaymentMethod(),
                sale.getSubtotal(),
                sale.getDiscountTotal(),
                sale.getOrderDiscount(),
                sale.getTaxTotal(),
                sale.getTotal(),
                sale.getAmountTendered(),
                sale.getChangeDue(),
                sale.getCashierName(),
                sale.getNote(),
                sale.getVoidReason(),
                sale.getVoidedAt(),
                CustomerResponse.from(sale.getCustomer()),
                sale.totalUnits(),
                sale.getItems().stream().map(SaleItemResponse::from).toList()
        );
    }
}
