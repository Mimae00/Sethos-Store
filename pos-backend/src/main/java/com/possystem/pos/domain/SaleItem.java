package com.possystem.pos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line on a receipt.
 *
 * <p>The product name, SKU, unit price and tax rate are snapshotted at sale time. The
 * product reference is kept only so reports can roll up by item; renaming or repricing a
 * product never rewrites history.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "sale_items",
        indexes = {
                @Index(name = "idx_sale_items_sale", columnList = "sale_id"),
                @Index(name = "idx_sale_items_product", columnList = "product_id")
        }
)
public class SaleItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sale_items_sale"))
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_sale_items_product"))
    private Product product;

    @Column(name = "product_name", nullable = false, length = 160)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 40)
    private String productSku;

    @Column(name = "unit", nullable = false, length = 16)
    private String unit = "pc";

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    /** unitPrice * quantity. */
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** grossAmount - discountAmount. */
    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** netAmount + taxAmount. */
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
