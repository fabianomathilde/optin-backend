package com.hexagonal.optin.adapters.in.rest;

/** HTTP response body for POST /opt-ins/validity-checks (see optin-api.yaml). */
public record ValidityCheckResponse(int revokedCount) {
}
