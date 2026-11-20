package com.hexagonal.optin.application.port.in;

import com.hexagonal.optin.domain.Frequency;

import java.time.LocalDate;

public record CreateOptInCommand(
        String merchant,
        String grantee,
        Frequency frequency,
        LocalDate startDate,
        LocalDate validUntil) {
}
