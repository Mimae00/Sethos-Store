package com.possystem.pos.exception;

import java.util.List;

/**
 * Maps to HTTP 422. The request was well formed but the operation is not allowed,
 * e.g. voiding an already voided sale or tendering less than the total.
 */
public class BusinessRuleException extends RuntimeException {

    private final List<String> details;

    public BusinessRuleException(String message) {
        this(message, List.of());
    }

    public BusinessRuleException(String message, List<String> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
