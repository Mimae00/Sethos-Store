package com.possystem.pos.repository.projection;

import java.math.BigDecimal;

/**
 * Raw aggregate straight out of the database. Sum columns are null when no sale matched
 * the filter, so callers must normalise before doing arithmetic.
 */
public record SaleTotalsRow(
        long saleCount,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal total
) {
}
