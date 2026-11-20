package com.hexagonal.optin.domain;

/** R1: the grantee already has an ACTIVE opt-in for this merchant. */
public class DuplicateActiveOptInException extends DomainException {

    public DuplicateActiveOptInException() {
        super("Grantee already has an ACTIVE opt-in for this merchant");
    }
}
