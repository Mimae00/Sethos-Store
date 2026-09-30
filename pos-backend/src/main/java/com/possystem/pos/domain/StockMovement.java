package com.possystem.pos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only journal of stock changes. Every mutation of
 * {@link Product#getStockQuantity()} writes one row here, which makes discrepancies
 * traceable instead of mysterious.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "stock_movements",
        indexes = {
                @Index(name = "idx_stock_movements_product", columnList = "product_id"),
                @Index(name = "idx_stock_movements_created", columnList = "created_at")
        }
)
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_stock_movements_product"))
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private StockMovementType type;

    /** Signed delta: negative for sales and shrinkage, positive for deliveries and returns. */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "reference", length = 32)
    private String reference;

    @Column(name = "reason", length = 255)
    private String reason;

    public static StockMovement of(Product product,
                                   StockMovementType type,
                                   int quantityChange,
                                   int quantityBefore,
                                   String reference,
                                   String reason) {
        StockMovement movement = new StockMovement();
        movement.product = product;
        movement.type = type;
        movement.quantityChange = quantityChange;
        movement.quantityBefore = quantityBefore;
        movement.quantityAfter = quantityBefore + quantityChange;
        movement.reference = reference;
        movement.reason = reason;
        return movement;
    }
}
