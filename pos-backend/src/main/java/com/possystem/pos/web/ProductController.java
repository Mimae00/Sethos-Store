package com.possystem.pos.web;

import com.possystem.pos.dto.PageResponse;
import com.possystem.pos.dto.ProductRequest;
import com.possystem.pos.dto.ProductResponse;
import com.possystem.pos.dto.StockAdjustmentRequest;
import com.possystem.pos.dto.StockMovementResponse;
import com.possystem.pos.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    /**
     * Catalogue search. {@code sort} accepts {@code field,direction} pairs, e.g.
     * {@code sort=name,asc}.
     */
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return products.search(search, categoryId, active, lowStock, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return products.findById(id);
    }

    @GetMapping("/by-barcode/{barcode}")
    public ProductResponse getByBarcode(@PathVariable String barcode) {
        return products.findByBarcode(barcode);
    }

    @GetMapping("/by-sku/{sku}")
    public ProductResponse getBySku(@PathVariable String sku) {
        return products.findBySku(sku);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock(@RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit) {
        return products.findLowStock(PageRequest.of(0, limit));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        ProductResponse created = products.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/products/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return products.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        products.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Signed stock correction. Negative values reduce stock. */
    @PostMapping("/{id}/stock-adjustments")
    public ProductResponse adjustStock(@PathVariable Long id,
                                       @Valid @RequestBody StockAdjustmentRequest request) {
        return products.adjustStock(id, request);
    }

    @GetMapping("/{id}/stock-movements")
    public PageResponse<StockMovementResponse> movements(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return products.movements(id, PageRequest.of(page, size));
    }

    /**
     * Whitelists sortable columns. Passing an arbitrary property through to JPA would let a
     * caller sort by an unmapped field and get a 500 back.
     */
    private Sort parseSort(String sort) {
        List<String> allowed = List.of("name", "sku", "price", "stockQuantity", "createdAt", "updatedAt");
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!allowed.contains(property)) {
            property = "name";
        }
        Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
