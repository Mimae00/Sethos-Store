package com.possystem.pos.domain;

/**
 * Reason a product's stock level changed.
 */
public enum StockMovementType {
    /** Initial load or supplier delivery. */
    PURCHASE,
    /** Manual correction after a physical count. */
    ADJUSTMENT,
    /** Stock leaving through the till. */
    SALE,
    /** Stock returning because a sale was voided or refunded. */
    RETURN,
    /** Damage, spoilage, theft. */
    SHRINKAGE
}
