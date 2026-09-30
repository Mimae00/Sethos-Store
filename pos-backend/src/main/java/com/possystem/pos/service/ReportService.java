package com.possystem.pos.service;

import com.possystem.pos.dto.ProductResponse;
import com.possystem.pos.dto.report.DailySalesPoint;
import com.possystem.pos.dto.report.DashboardResponse;
import com.possystem.pos.dto.report.PaymentMethodTotalResponse;
import com.possystem.pos.dto.report.SalesSummaryResponse;
import com.possystem.pos.dto.report.TopProductResponse;
import com.possystem.pos.exception.BusinessRuleException;
import com.possystem.pos.repository.ProductRepository;
import com.possystem.pos.repository.SaleItemRepository;
import com.possystem.pos.repository.SaleRepository;
import com.possystem.pos.repository.projection.SaleItemTotalsRow;
import com.possystem.pos.repository.projection.SaleTotalsRow;
import com.possystem.pos.support.Money;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only reporting over COMPLETED sales.
 *
 * <p>Ranges are expressed as inclusive calendar dates and converted to a half-open instant
 * window {@code [startOfFrom, startOfTo+1)} in the configured store time zone. Half-open
 * avoids the classic bug where a sale at 23:59:59.500 falls outside an inclusive upper
 * bound.</p>
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    /** Guard rail: a wider window would pull an unbounded number of rows into memory. */
    private static final int MAX_RANGE_DAYS = 400;
    private static final int DEFAULT_TOP_PRODUCTS = 5;

    private final SaleRepository sales;
    private final SaleItemRepository saleItems;
    private final ProductRepository products;
    private final Clock clock;
    private final ZoneId storeZone;

    public ReportService(SaleRepository sales,
                         SaleItemRepository saleItems,
                         ProductRepository products,
                         Clock clock) {
        this.sales = sales;
        this.saleItems = saleItems;
        this.products = products;
        this.clock = clock;
        this.storeZone = clock.getZone();
    }

    public SalesSummaryResponse summary(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? today() : from;
        LocalDate end = to == null ? today() : to;
        validateRange(start, end);
        return buildSummary(start, end);
    }

    public List<TopProductResponse> topProducts(LocalDate from, LocalDate to, int limit) {
        LocalDate start = from == null ? today().minusDays(29) : from;
        LocalDate end = to == null ? today() : to;
        validateRange(start, end);
        int capped = Math.min(Math.max(limit, 1), 100);

        return saleItems.topProductsBetween(startInstant(start), endInstant(end), PageRequest.of(0, capped))
                .stream()
                .map(row -> new TopProductResponse(
                        row.productId(),
                        row.productSku(),
                        row.productName(),
                        row.quantitySold() == null ? 0L : row.quantitySold(),
                        Money.of(row.revenue())))
                .toList();
    }

    public List<DailySalesPoint> dailySales(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? today().minusDays(13) : from;
        LocalDate end = to == null ? today() : to;
        validateRange(start, end);

        Map<LocalDate, long[]> counts = new HashMap<>();
        Map<LocalDate, BigDecimal> totals = new HashMap<>();

        for (Object[] row : sales.soldAtAndTotalBetween(startInstant(start), endInstant(end))) {
            LocalDate day = ((Instant) row[0]).atZone(storeZone).toLocalDate();
            counts.computeIfAbsent(day, key -> new long[1])[0]++;
            totals.merge(day, Money.of((BigDecimal) row[1]), Money::add);
        }

        List<DailySalesPoint> points = new ArrayList<>();
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            long[] count = counts.get(cursor);
            points.add(new DailySalesPoint(
                    cursor,
                    count == null ? 0L : count[0],
                    totals.getOrDefault(cursor, Money.ZERO)));
        }
        return points;
    }

    public List<PaymentMethodTotalResponse> paymentBreakdown(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? today() : from;
        LocalDate end = to == null ? today() : to;
        validateRange(start, end);

        return sales.totalsByPaymentMethod(startInstant(start), endInstant(end)).stream()
                .map(row -> new PaymentMethodTotalResponse(
                        row.paymentMethod(),
                        row.saleCount() == null ? 0L : row.saleCount(),
                        Money.of(row.total())))
                .toList();
    }

    /** Everything the landing screen needs, assembled in one call. */
    public DashboardResponse dashboard() {
        LocalDate today = today();
        return new DashboardResponse(
                buildSummary(today, today),
                buildSummary(today.minusDays(6), today),
                buildSummary(today.withDayOfMonth(1), today),
                products.countActive(),
                products.countLowStock(),
                Money.of(products.inventoryCostValue()),
                dailySales(today.minusDays(13), today),
                topProducts(today.minusDays(29), today, DEFAULT_TOP_PRODUCTS),
                paymentBreakdown(today, today),
                products.findLowStock(PageRequest.of(0, 8)).stream().map(ProductResponse::from).toList());
    }

    private SalesSummaryResponse buildSummary(LocalDate from, LocalDate to) {
        Instant start = startInstant(from);
        Instant end = endInstant(to);

        SaleTotalsRow totals = sales.totalsBetween(start, end);
        SaleItemTotalsRow itemTotals = saleItems.totalsBetween(start, end);

        long saleCount = totals == null ? 0L : totals.saleCount();
        BigDecimal grossSales = totals == null ? Money.ZERO : Money.of(totals.subtotal());
        BigDecimal discountTotal = totals == null ? Money.ZERO : Money.of(totals.discountTotal());
        BigDecimal taxTotal = totals == null ? Money.ZERO : Money.of(totals.taxTotal());
        BigDecimal totalCollected = totals == null ? Money.ZERO : Money.of(totals.total());

        long unitsSold = itemTotals == null || itemTotals.unitsSold() == null ? 0L : itemTotals.unitsSold();
        BigDecimal cogs = itemTotals == null ? Money.ZERO : Money.of(itemTotals.costOfGoodsSold());

        // Revenue excluding tax. Derived from gross minus discounts rather than from
        // totalCollected minus tax, so the ladder in SalesSummaryResponse always adds up.
        BigDecimal netSales = Money.atLeastZero(Money.subtract(grossSales, discountTotal));
        BigDecimal grossProfit = Money.subtract(netSales, cogs);

        return new SalesSummaryResponse(
                from,
                to,
                saleCount,
                unitsSold,
                grossSales,
                discountTotal,
                netSales,
                taxTotal,
                totalCollected,
                cogs,
                grossProfit,
                saleCount == 0 ? Money.ZERO : Money.divide(totalCollected, saleCount));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("The 'from' date must not be after the 'to' date");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BusinessRuleException(
                    "Range is too wide (" + days + " days). The maximum is " + MAX_RANGE_DAYS + " days.");
        }
    }

    private Instant startInstant(LocalDate date) {
        return date.atStartOfDay(storeZone).toInstant();
    }

    /** Exclusive upper bound: midnight at the start of the day after {@code date}. */
    private Instant endInstant(LocalDate date) {
        return date.plusDays(1).atStartOfDay(storeZone).toInstant();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
