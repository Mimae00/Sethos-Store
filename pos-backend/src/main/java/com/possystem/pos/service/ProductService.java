package com.possystem.pos.service;

import com.possystem.pos.domain.Category;
import com.possystem.pos.domain.Product;
import com.possystem.pos.domain.StockMovementType;
import com.possystem.pos.dto.PageResponse;
import com.possystem.pos.dto.ProductRequest;
import com.possystem.pos.dto.ProductResponse;
import com.possystem.pos.dto.StockAdjustmentRequest;
import com.possystem.pos.dto.StockMovementResponse;
import com.possystem.pos.exception.BusinessRuleException;
import com.possystem.pos.exception.DuplicateResourceException;
import com.possystem.pos.exception.ResourceNotFoundException;
import com.possystem.pos.repository.CategoryRepository;
import com.possystem.pos.repository.ProductRepository;
import com.possystem.pos.repository.SaleItemRepository;
import com.possystem.pos.repository.spec.ProductSpecifications;
import com.possystem.pos.support.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final SaleItemRepository saleItems;
    private final StockService stock;

    public ProductService(ProductRepository products,
                          CategoryRepository categories,
                          SaleItemRepository saleItems,
                          StockService stock) {
        this.products = products;
        this.categories = categories;
        this.saleItems = saleItems;
        this.stock = stock;
    }

    public PageResponse<ProductResponse> search(String term,
                                                Long categoryId,
                                                Boolean active,
                                                Boolean lowStock,
                                                Pageable pageable) {
        Page<Product> page = products.findAll(
                ProductSpecifications.search(blankToNull(term), categoryId, active, lowStock),
                pageable);
        return PageResponse.of(page, ProductResponse::from);
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(require(id));
    }

    /** Barcode scan path used by the terminal. */
    public ProductResponse findByBarcode(String barcode) {
        return ProductResponse.from(products.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("No product with barcode " + barcode)));
    }

    public ProductResponse findBySku(String sku) {
        return ProductResponse.from(products.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> new ResourceNotFoundException("No product with SKU " + sku)));
    }

    public List<ProductResponse> findLowStock(Pageable pageable) {
        return products.findLowStock(pageable).stream().map(ProductResponse::from).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim();
        String barcode = blankToNull(request.barcode());

        if (products.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("SKU '" + sku + "' is already in use");
        }
        if (barcode != null && products.existsByBarcode(barcode)) {
            throw new DuplicateResourceException("Barcode '" + barcode + "' is already in use");
        }

        Product product = new Product();
        apply(product, request, sku, barcode);
        product.setStockQuantity(0);
        Product saved = products.save(product);

        // Opening stock goes in as a movement so the journal can explain the whole balance.
        int openingStock = request.stockQuantity() == null ? 0 : request.stockQuantity();
        if (openingStock != 0 && saved.isTrackStock()) {
            stock.applyChange(saved, openingStock, StockMovementType.PURCHASE, null, "Opening stock");
        }
        return ProductResponse.from(saved);
    }

    /**
     * Updates the catalogue fields only. {@code stockQuantity} in the payload is ignored;
     * stock moves exclusively through {@link #adjustStock} or a checkout so that every
     * change lands in the journal.
     */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = require(id);

        String sku = request.sku().trim();
        String barcode = blankToNull(request.barcode());

        products.findBySkuIgnoreCase(sku).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("SKU '" + sku + "' is already in use");
            }
        });
        if (barcode != null) {
            products.findByBarcode(barcode).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new DuplicateResourceException("Barcode '" + barcode + "' is already in use");
                }
            });
        }

        apply(product, request, sku, barcode);
        return ProductResponse.from(products.save(product));
    }

    /**
     * Products that already appear in sale history are deactivated rather than deleted, so
     * receipts keep their product link. Never-sold products are removed outright.
     */
    @Transactional
    public void delete(Long id) {
        Product product = products.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));

        if (saleItems.existsByProductId(id)) {
            if (!product.isActive()) {
                throw new BusinessRuleException(
                        "'" + product.getName() + "' appears in past sales and is already deactivated.");
            }
            product.setActive(false);
            products.save(product);
            return;
        }
        products.delete(product);
    }

    @Transactional
    public ProductResponse adjustStock(Long id, StockAdjustmentRequest request) {
        Product product = require(id);

        if (!product.isTrackStock()) {
            throw new BusinessRuleException("'" + product.getName() + "' does not track stock");
        }
        if (request.quantityChange() == 0) {
            throw new BusinessRuleException("Quantity change cannot be zero");
        }
        if (request.type() == StockMovementType.SALE) {
            throw new BusinessRuleException("SALE movements are only created by a checkout");
        }

        int resulting = product.getStockQuantity() + request.quantityChange();
        if (resulting < 0) {
            throw new BusinessRuleException("Adjustment would take stock of '" + product.getName()
                    + "' to " + resulting + ". On hand is " + product.getStockQuantity() + ".");
        }

        stock.applyChange(product, request.quantityChange(), request.type(), null, request.reason());
        return ProductResponse.from(product);
    }

    public PageResponse<StockMovementResponse> movements(Long productId, Pageable pageable) {
        if (productId != null && !products.existsById(productId)) {
            throw ResourceNotFoundException.of("Product", productId);
        }
        return stock.history(productId, pageable);
    }

    private Product require(Long id) {
        return products.findWithCategoryById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private void apply(Product product, ProductRequest request, String sku, String barcode) {
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setName(request.name().trim());
        product.setDescription(blankToNull(request.description()));
        product.setPrice(Money.of(request.price()));
        product.setCost(Money.of(request.cost()));
        product.setTaxRate(request.taxRate() == null ? Money.ZERO : request.taxRate());
        product.setReorderLevel(request.reorderLevel() == null ? 0 : request.reorderLevel());
        product.setUnit(request.unit() == null || request.unit().isBlank() ? "pc" : request.unit().trim());
        product.setImageUrl(blankToNull(request.imageUrl()));
        product.setActive(request.active() == null || request.active());
        product.setTrackStock(request.trackStock() == null || request.trackStock());
        product.setCategory(resolveCategory(request.categoryId()));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categories.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
