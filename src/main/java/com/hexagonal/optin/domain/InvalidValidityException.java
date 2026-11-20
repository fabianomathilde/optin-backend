package com.hexagonal.optin.domain;

/** R3, R4, R5: the requested validity period is not allowed. */
public class InvalidValidityException extends DomainException {

    public InvalidValidityException(String message) {
        super(message);
    }
}
