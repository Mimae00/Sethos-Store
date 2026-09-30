package com.possystem.pos.repository.projection;

import java.math.BigDecimal;

/**
 * Line level aggregate used to derive units sold and cost of goods sold.
 */
public record SaleItemTotalsRow(
        Long unitsSold,
        BigDecimal costOfGoodsSold
) {
}
