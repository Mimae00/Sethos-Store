package com.possystem.pos.exception;

/** Maps to HTTP 409. Raised for unique constraints the user can actually fix, like SKUs. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
