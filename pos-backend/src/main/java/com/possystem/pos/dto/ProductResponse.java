package com.possystem.pos.dto;

import com.possystem.pos.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String barcode,
        String name,
        String description,
        BigDecimal price,
        BigDecimal cost,
        BigDecimal taxRate,
        Integer stockQuantity,
        Integer reorderLevel,
        String unit,
        String imageUrl,
        boolean active,
        boolean trackStock,
        boolean lowStock,
        CategoryResponse category,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getBarcode(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCost(),
                product.getTaxRate(),
                product.getStockQuantity(),
                product.getReorderLevel(),
                product.getUnit(),
                product.getImageUrl(),
                product.isActive(),
                product.isTrackStock(),
                product.isLowStock(),
                CategoryResponse.from(product.getCategory()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
