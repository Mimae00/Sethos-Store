package com.possystem.pos.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A completed transaction at the till.
 *
 * <p>Totals are stored rather than recomputed on read: prices and tax rates change over
 * time and a receipt must always reproduce the numbers the customer actually paid.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "sales",
        uniqueConstraints = @UniqueConstraint(name = "uk_sales_reference", columnNames = "reference"),
        indexes = {
                @Index(name = "idx_sales_sold_at", columnList = "sold_at"),
                @Index(name = "idx_sales_status", columnList = "status")
        }
)
public class Sale extends BaseEntity {

    /** Human readable receipt number, e.g. S-20260927-0007. */
    @Column(name = "reference", nullable = false, length = 32)
    private String reference;

    /** Business timestamp of the sale, separate from the row's createdAt. */
    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SaleStatus status = SaleStatus.COMPLETED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    /** Sum of every line's gross amount, before any discount or tax. */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Line level discounts plus the order level discount. */
    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    /** Discount applied to the whole basket, after tax. */
    @Column(name = "order_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal orderDiscount = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    /** Amount actually due from the customer. */
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "amount_tendered", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountTendered = BigDecimal.ZERO;

    @Column(name = "change_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal changeDue = BigDecimal.ZERO;

    /** Free text until authentication lands; then it becomes the signed-in user. */
    @Column(name = "cashier_name", nullable = false, length = 120)
    private String cashierName = "Staff";

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_sales_customer"))
    private Customer customer;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<SaleItem> items = new ArrayList<>();

    public void addItem(SaleItem item) {
        item.setSale(this);
        this.items.add(item);
    }

    public int totalUnits() {
        return items.stream().mapToInt(SaleItem::getQuantity).sum();
    }
}
