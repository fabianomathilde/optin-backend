package com.hexagonal.optin.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptInTest {

    private static final String MERCHANT = "11222333000181";
    private static final String GRANTEE = "00360305000104";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Test
    void grantedOptInIsActiveWithoutRevocation() {
        OptIn optIn = OptIn.grant(MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY, TODAY.plusMonths(6), TODAY);

        assertThat(optIn.isActive()).isTrue();
        assertThat(optIn.revokedAt()).isNull();
        assertThat(optIn.id()).isNull();
    }

    @Test
    void effectiveUpToAndIncludingValidUntilDay() {
        OptIn optIn = OptIn.grant(MERCHANT, GRANTEE, Frequency.WEEKLY,
                TODAY, TODAY.plusMonths(6), TODAY);
        LocalDate validUntil = optIn.validUntil();

        assertThat(optIn.isEffectiveOn(validUntil)).isTrue();                  // R7: D <= validUntil
        assertThat(optIn.isEffectiveOn(validUntil.plusDays(1))).isFalse();
    }

    @Test
    void revokedOptInIsNotEffective() {
        OptIn revoked = OptIn.grant(MERCHANT, GRANTEE, Frequency.MONTHLY, TODAY, TODAY.plusMonths(6), TODAY)
                .revoke(Instant.parse("2026-11-19T12:00:00Z"));

        assertThat(revoked.isEffectiveOn(TODAY)).isFalse();                    // R7: must be ACTIVE
    }

    @Test
    void revokedOptInCannotBeRevokedAgain() {
        OptIn revoked = OptIn.grant(MERCHANT, GRANTEE, Frequency.MONTHLY, TODAY, TODAY.plusMonths(6), TODAY)
                .revoke(Instant.parse("2026-11-19T12:00:00Z"));

        assertThatThrownBy(() -> revoked.revoke(Instant.parse("2026-11-20T12:00:00Z")))
                .isInstanceOf(OptInAlreadyRevokedException.class);             // R6
    }

    @Test
    void grantRejectsStartDateBeforeToday() {                                  // R10
        assertThatThrownBy(() -> OptIn.grant(MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY.minusDays(1), TODAY.plusMonths(3), TODAY))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void grantAcceptsStartDateToday() {                                        // R10 boundary
        assertThat(OptIn.grant(MERCHANT, GRANTEE, Frequency.DAILY, TODAY, TODAY.plusMonths(3), TODAY).startDate())
                .isEqualTo(TODAY);
    }

    @Test
    void grantRejectsValidUntilNotAfterToday() {                               // R3
        assertThatThrownBy(() -> OptIn.grant(MERCHANT, GRANTEE, Frequency.DAILY, TODAY, TODAY, TODAY))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void grantRejectsStartDateAfterValidUntil() {                              // R5
        assertThatThrownBy(() -> OptIn.grant(MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY.plusMonths(2), TODAY.plusMonths(1), TODAY))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void grantCapsValidityAtOneYearAfterStartDate() {                          // R4
        LocalDate start = TODAY.plusMonths(4);

        assertThat(OptIn.grant(MERCHANT, GRANTEE, Frequency.MONTHLY, start, start.plusYears(1), TODAY).validUntil())
                .isEqualTo(start.plusYears(1));
        assertThatThrownBy(() -> OptIn.grant(MERCHANT, GRANTEE, Frequency.MONTHLY,
                start, start.plusYears(1).plusDays(1), TODAY))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void expiredOnlyAfterValidUntilDay() {
        OptIn optIn = OptIn.grant(MERCHANT, GRANTEE, Frequency.DAILY, TODAY, TODAY.plusMonths(6), TODAY);

        assertThat(optIn.isExpiredOn(optIn.validUntil())).isFalse();
        assertThat(optIn.isExpiredOn(optIn.validUntil().plusDays(1))).isTrue();  // R8
    }
}
