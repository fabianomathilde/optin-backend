package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.domain.Frequency;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** HTTP response body for an opt-in (see optin-api.yaml, schema OptIn). */
public record OptInResponse(
        UUID id,
        String merchant,
        String grantee,
        Frequency frequency,
        LocalDate startDate,
        LocalDate validUntil,
        OptInStatus status,
        Instant revokedAt) {

    public static OptInResponse from(OptIn optIn) {
        return new OptInResponse(
                optIn.id(),
                optIn.merchant(),
                optIn.grantee(),
                optIn.frequency(),
                optIn.startDate(),
                optIn.validUntil(),
                optIn.status(),
                optIn.revokedAt());
    }
}
