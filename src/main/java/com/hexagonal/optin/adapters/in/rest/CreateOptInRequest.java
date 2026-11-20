package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.domain.Frequency;

import java.time.LocalDate;

/** HTTP request body for POST /opt-ins (see optin-api.yaml). */
public record CreateOptInRequest(
        String merchant,
        String grantee,
        Frequency frequency,
        LocalDate startDate,
        LocalDate validUntil) {
}
