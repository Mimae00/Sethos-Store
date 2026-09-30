package com.possystem.pos.exception;

import java.util.List;

/**
 * Raised when a checkout asks for more units than are on hand. Carries a line per
 * offending product so the terminal can highlight exactly which rows to fix.
 */
public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(List<String> details) {
        super("Not enough stock to complete this sale", details);
    }
}
