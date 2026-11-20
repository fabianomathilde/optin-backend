package com.hexagonal.optin.domain;

import java.util.UUID;

/** R9: no opt-in with the given id. */
public class OptInNotFoundException extends DomainException {

    public OptInNotFoundException(UUID id) {
        super("Opt-in " + id + " not found");
    }
}
