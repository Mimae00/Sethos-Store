package com.possystem.pos.dto.report;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String sku,
        String name,
        long quantitySold,
        BigDecimal revenue
) {
}
