package com.possystem.pos.domain;

/**
 * How the customer settled the sale. CASH is the only method that produces change.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    EWALLET,
    BANK_TRANSFER,
    OTHER;

    public boolean requiresTender() {
        return this == CASH;
    }
}
