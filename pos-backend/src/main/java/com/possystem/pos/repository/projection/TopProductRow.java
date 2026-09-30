package com.possystem.pos.repository.projection;

import java.math.BigDecimal;

/**
 * Best sellers over a period, keyed on the snapshotted SKU so deleted products still
 * show up in history.
 */
public record TopProductRow(
        Long productId,
        String productSku,
        String productName,
        Long quantitySold,
        BigDecimal revenue
) {
}
