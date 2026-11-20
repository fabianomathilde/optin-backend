package com.hexagonal.optin.domain;

/** R6: the opt-in is already revoked. */
public class OptInAlreadyRevokedException extends DomainException {

    public OptInAlreadyRevokedException(Integer id) {
        super("Opt-in " + id + " is already revoked");
    }
}
