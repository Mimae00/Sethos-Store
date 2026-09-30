package com.possystem.pos.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One bar on the sales chart. Days with no sales are included with zeros so the
 * front end does not have to fill gaps.
 */
public record DailySalesPoint(
        LocalDate date,
        long saleCount,
        BigDecimal total
) {
}
