package com.possystem.pos.dto.report;

import com.possystem.pos.dto.ProductResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the landing screen needs, in one round trip.
 */
public record DashboardResponse(
        SalesSummaryResponse today,
        SalesSummaryResponse last7Days,
        SalesSummaryResponse thisMonth,
        long activeProducts,
        long lowStockCount,
        BigDecimal inventoryCostValue,
        List<DailySalesPoint> dailySales,
        List<TopProductResponse> topProducts,
        List<PaymentMethodTotalResponse> paymentBreakdown,
        List<ProductResponse> lowStockProducts
) {
}
