package com.possystem.pos.web;

import com.possystem.pos.dto.report.DailySalesPoint;
import com.possystem.pos.dto.report.DashboardResponse;
import com.possystem.pos.dto.report.PaymentMethodTotalResponse;
import com.possystem.pos.dto.report.SalesSummaryResponse;
import com.possystem.pos.dto.report.TopProductResponse;
import com.possystem.pos.service.ReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporting endpoints. Date parameters are inclusive calendar dates in the store's time
 * zone; omitting them falls back to a sensible recent window per report.
 */
@Validated
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    /** One-shot payload for the landing screen. */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return reports.dashboard();
    }

    @GetMapping("/summary")
    public SalesSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.summary(from, to);
    }

    @GetMapping("/daily-sales")
    public List<DailySalesPoint> dailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.dailySales(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return reports.topProducts(from, to, limit);
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodTotalResponse> paymentMethods(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.paymentBreakdown(from, to);
    }
}
