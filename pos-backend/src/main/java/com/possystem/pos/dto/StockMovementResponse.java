package com.possystem.pos.dto;

import com.possystem.pos.domain.StockMovement;
import com.possystem.pos.domain.StockMovementType;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        StockMovementType type,
        Integer quantityChange,
        Integer quantityBefore,
        Integer quantityAfter,
        String reference,
        String reason,
        Instant createdAt
) {

    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getSku(),
                movement.getType(),
                movement.getQuantityChange(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getReference(),
                movement.getReason(),
                movement.getCreatedAt()
        );
    }
}
