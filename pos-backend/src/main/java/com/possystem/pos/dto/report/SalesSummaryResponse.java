package com.possystem.pos.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregate takings for an inclusive date range. Only COMPLETED sales are counted;
 * voided and refunded sales are excluded entirely.
 *
 * <p>The money fields follow the usual accounting ladder, so they can be read top to
 * bottom:</p>
 * <pre>
 *   grossSales       sum of line amounts at list price, excluding tax
 * - discountTotal    line discounts plus order discounts
 * = netSales         revenue, excluding tax
 * + taxTotal         tax charged on top
 * = totalCollected   what customers actually paid
 * </pre>
 */
public record SalesSummaryResponse(
        LocalDate from,
        LocalDate to,
        long saleCount,
        long unitsSold,
        BigDecimal grossSales,
        BigDecimal discountTotal,
        BigDecimal netSales,
        BigDecimal taxTotal,
        BigDecimal totalCollected,
        BigDecimal costOfGoodsSold,
        /** netSales - costOfGoodsSold. Tax is collected on the state's behalf, not earned. */
        BigDecimal grossProfit,
        BigDecimal averageSaleValue
) {
}
