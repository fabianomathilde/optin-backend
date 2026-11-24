package com.hexagonal.optin.domain;

import java.util.UUID;

/** R6: the opt-in is already revoked. */
public class OptInAlreadyRevokedException extends DomainException {

    public OptInAlreadyRevokedException(UUID id) {
        super("Opt-in " + id + " is already revoked");
    }
}
