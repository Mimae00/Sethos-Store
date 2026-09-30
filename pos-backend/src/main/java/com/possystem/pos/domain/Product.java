package com.possystem.pos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A sellable item. Stock is tracked on the product row itself and every movement
 * is journalled in {@link StockMovement} so the count can always be explained.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
                @UniqueConstraint(name = "uk_products_barcode", columnNames = "barcode")
        },
        indexes = {
                @Index(name = "idx_products_name", columnList = "name"),
                @Index(name = "idx_products_category", columnList = "category_id")
        }
)
public class Product extends BaseEntity {

    @Column(name = "sku", nullable = false, length = 40)
    private String sku;

    /** Optional scannable code. Null when the item is keyed in manually. */
    @Column(name = "barcode", length = 64)
    private String barcode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /** Tax-exclusive selling price. */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** Purchase cost, used for the margin figures on the dashboard. */
    @Column(name = "cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    /** Percentage, e.g. 12.00 for 12% VAT. */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    /** Dashboard flags the product once stock drops to this level. */
    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 0;

    /** Unit of measure label shown on the receipt, e.g. pc, kg, box. */
    @Column(name = "unit", nullable = false, length = 16)
    private String unit = "pc";

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Inactive products stay in history but disappear from the terminal. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** When false the item is a service and stock is never decremented. */
    @Column(name = "track_stock", nullable = false)
    private boolean trackStock = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_products_category"))
    private Category category;

    public boolean isLowStock() {
        return trackStock && stockQuantity != null && reorderLevel != null && stockQuantity <= reorderLevel;
    }
}
