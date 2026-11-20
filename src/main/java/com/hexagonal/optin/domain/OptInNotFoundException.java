package com.hexagonal.optin.domain;

/** R9: no opt-in with the given id. */
public class OptInNotFoundException extends DomainException {

    public OptInNotFoundException(int id) {
        super("Opt-in " + id + " not found");
    }
}
