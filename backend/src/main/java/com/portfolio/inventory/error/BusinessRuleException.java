package com.portfolio.inventory.error;

/** A request that is well-formed but violates a domain rule; reported against a single field. */
public class BusinessRuleException extends RuntimeException {

    private final String field;

    public BusinessRuleException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
