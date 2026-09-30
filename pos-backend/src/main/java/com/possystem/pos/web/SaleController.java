package com.possystem.pos.web;

import com.possystem.pos.domain.SaleStatus;
import com.possystem.pos.dto.CheckoutRequest;
import com.possystem.pos.dto.PageResponse;
import com.possystem.pos.dto.SaleResponse;
import com.possystem.pos.dto.SaleSummaryResponse;
import com.possystem.pos.dto.VoidSaleRequest;
import com.possystem.pos.service.SaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Validated
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService sales;
    private final ZoneId storeZone;

    public SaleController(SaleService sales, Clock clock) {
        this.sales = sales;
        this.storeZone = clock.getZone();
    }

    /**
     * Sales history. {@code from} and {@code to} are inclusive calendar dates in the
     * store's time zone.
     */
    @GetMapping
    public PageResponse<SaleSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {

        return sales.search(
                search,
                status,
                startOfDay(from),
                startOfNextDay(to),
                PageRequest.of(page, size, Sort.by("soldAt").descending()));
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable Long id) {
        return sales.findById(id);
    }

    @GetMapping("/by-reference/{reference}")
    public SaleResponse getByReference(@PathVariable String reference) {
        return sales.findByReference(reference);
    }

    /** Rings up a basket and returns the full receipt. */
    @PostMapping
    public ResponseEntity<SaleResponse> checkout(@Valid @RequestBody CheckoutRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        SaleResponse sale = sales.checkout(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/sales/{id}").buildAndExpand(sale.id()).toUri())
                .body(sale);
    }

    @PostMapping("/{id}/void")
    public SaleResponse voidSale(@PathVariable Long id, @Valid @RequestBody VoidSaleRequest request) {
        return sales.voidSale(id, request.reason());
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(storeZone).toInstant();
    }

    /** Exclusive upper bound so the whole of {@code to} is included. */
    private Instant startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(storeZone).toInstant();
    }
}
