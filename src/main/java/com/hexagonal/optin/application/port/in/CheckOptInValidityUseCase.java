package com.hexagonal.optin.application.port.in;

/** Driving port: revoke the opt-ins whose validUntil has passed (R8). Run once a day. */
public interface CheckOptInValidityUseCase {

    /** @return the number of opt-ins revoked by this check */
    int revokeExpired();
}
