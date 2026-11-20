package com.hexagonal.optin.domain;

/** Base class for violations of the domain rules in ADR 0003. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
