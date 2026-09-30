package com.possystem.pos.dto;

import com.possystem.pos.domain.SaleItem;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        String unit,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal grossAmount,
        BigDecimal discountAmount,
        BigDecimal netAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal lineTotal
) {

    public static SaleItemResponse from(SaleItem item) {
        return new SaleItemResponse(
                item.getId(),
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProductName(),
                item.getProductSku(),
                item.getUnit(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getGrossAmount(),
                item.getDiscountAmount(),
                item.getNetAmount(),
                item.getTaxRate(),
                item.getTaxAmount(),
                item.getLineTotal()
        );
    }
}
