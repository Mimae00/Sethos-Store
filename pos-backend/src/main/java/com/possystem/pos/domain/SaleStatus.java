package com.possystem.pos.domain;

/**
 * Lifecycle of a sale. Sales are never deleted; they are voided or refunded so the
 * audit trail and the stock journal stay consistent.
 */
public enum SaleStatus {
    COMPLETED,
    VOIDED,
    REFUNDED
}
