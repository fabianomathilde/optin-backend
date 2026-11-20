package com.hexagonal.optin.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Opt-in aggregate. Holds the rules for granting, revoking and validity (see ADR 0003).
 * {@code id} is null until the opt-in is saved; the repository assigns a UUID (ADR 0005).
 */
public record OptIn(
        UUID id,
        String merchant,
        String grantee,
        Frequency frequency,
        LocalDate startDate,
        LocalDate validUntil,
        OptInStatus status,
        Instant revokedAt) {

    static final int MAX_VALIDITY_YEARS = 1;

    /** Creates a new ACTIVE opt-in after checking the validity rules R3, R4 and R5. */
    public static OptIn grant(String merchant, String grantee, Frequency frequency,
                              LocalDate startDate, LocalDate validUntil, LocalDate today) {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(validUntil, "validUntil");
        if (startDate.isBefore(today)) {
            throw new InvalidValidityException("startDate must not be before today");   // R10
        }
        if (!validUntil.isAfter(today)) {
            throw new InvalidValidityException("validUntil must be after today");       // R3
        }
        if (startDate.isAfter(validUntil)) {
            throw new InvalidValidityException("startDate must not be after validUntil"); // R5
        }
        if (validUntil.isAfter(startDate.plusYears(MAX_VALIDITY_YEARS))) {
            throw new InvalidValidityException("validUntil must be at most one year after startDate"); // R4
        }
        return new OptIn(null, merchant, grantee, frequency, startDate, validUntil,
                OptInStatus.ACTIVE, null);
    }

    /** Revokes the opt-in (R6): a revoked opt-in cannot be revoked again. */
    public OptIn revoke(Instant at) {
        if (status == OptInStatus.REVOKED) {
            throw new OptInAlreadyRevokedException(id);
        }
        return new OptIn(id, merchant, grantee, frequency, startDate, validUntil,
                OptInStatus.REVOKED, at);
    }

    /** Copy of this opt-in with the given id; used by repositories when they assign one. */
    public OptIn withId(UUID newId) {
        return new OptIn(newId, merchant, grantee, frequency, startDate, validUntil, status, revokedAt);
    }

    public boolean isActive() {
        return status == OptInStatus.ACTIVE;
    }

    /** True when validUntil has passed on the given day (R8). */
    public boolean isExpiredOn(LocalDate day) {
        return validUntil.isBefore(day);
    }

    /** R7: effective on day D only if ACTIVE and D is not after validUntil. */
    public boolean isEffectiveOn(LocalDate day) {
        return isActive() && !day.isAfter(validUntil);
    }
}
