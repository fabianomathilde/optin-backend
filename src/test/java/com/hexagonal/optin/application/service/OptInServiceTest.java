package com.hexagonal.optin.application.service;

import com.hexagonal.optin.application.fakes.InMemoryMerchantList;
import com.hexagonal.optin.application.fakes.InMemoryOptInRepository;
import com.hexagonal.optin.application.port.in.CreateOptInCommand;
import com.hexagonal.optin.application.port.in.RevokeOptInCommand;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import com.hexagonal.optin.domain.Frequency;
import com.hexagonal.optin.domain.InvalidValidityException;
import com.hexagonal.optin.domain.MerchantNotActiveException;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInAlreadyRevokedException;
import com.hexagonal.optin.domain.OptInNotFoundException;
import com.hexagonal.optin.domain.OptInStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptInServiceTest {

    private static final String MERCHANT = "11222333000181";
    private static final String OTHER_MERCHANT = "45543915000181";
    private static final String GRANTEE = "00360305000104";
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    private InMemoryOptInRepository repository;
    private InMemoryMerchantList merchants;
    private OptInService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOptInRepository();
        merchants = new InMemoryMerchantList().activate(MERCHANT).activate(OTHER_MERCHANT);
        service = new OptInService(repository, merchants, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsActiveOptInWithAssignedId() {
        OptIn created = service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(6)));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(OptInStatus.ACTIVE);
    }

    @Test
    void granteeCannotHaveTwoActiveOptInsForSameMerchant() {                   // R1
        service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(6)));

        assertThatThrownBy(() -> service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(3))))
                .isInstanceOf(DuplicateActiveOptInException.class);
    }

    @Test
    void sameGranteeCanHaveOptInForAnotherMerchant() {                         // R1 is per merchant
        service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(6)));

        OptIn other = service.create(command(OTHER_MERCHANT, GRANTEE, TODAY.plusMonths(6)));

        assertThat(other.merchant()).isEqualTo(OTHER_MERCHANT);
    }

    @Test
    void grantAfterRevocationIsAllowed() {                                     // R1 applies to ACTIVE only
        OptIn first = service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(6)));
        service.revoke(new RevokeOptInCommand(first.id()));

        OptIn second = service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(6)));

        assertThat(second.status()).isEqualTo(OptInStatus.ACTIVE);
        assertThat(second.id()).isNotEqualTo(first.id());
    }

    @Test
    void rejectsInactiveMerchant() {                                           // R2
        assertThatThrownBy(() -> service.create(command("99999999000199", GRANTEE, TODAY.plusMonths(6))))
                .isInstanceOf(MerchantNotActiveException.class);

        assertThat(repository.findByMerchant("99999999000199", Optional.empty())).isEmpty();
    }

    @Test
    void validUntilMustBeAfterToday() {                                        // R3
        assertThatThrownBy(() -> service.create(command(MERCHANT, GRANTEE, TODAY)))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void validUntilMayBeExactlyOneYearAway() {                                 // R4 (boundary)
        OptIn created = service.create(command(MERCHANT, GRANTEE, TODAY.plusYears(1)));

        assertThat(created.validUntil()).isEqualTo(TODAY.plusYears(1));
    }

    @Test
    void validUntilAboveOneYearIsRejected() {                                  // R4
        assertThatThrownBy(() -> service.create(command(MERCHANT, GRANTEE, TODAY.plusYears(1).plusDays(1))))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void startDateBeforeTodayIsRejected() {                                    // R10
        CreateOptInCommand command = new CreateOptInCommand(MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY.minusDays(1), TODAY.plusMonths(6));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void startDateTodayIsAllowed() {                                           // R10 (boundary)
        OptIn created = service.create(new CreateOptInCommand(MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY, TODAY.plusMonths(6)));

        assertThat(created.startDate()).isEqualTo(TODAY);
    }

    @Test
    void oneYearCapIsCountedFromStartDate() {                                  // R4
        LocalDate start = TODAY.plusMonths(3);
        OptIn created = service.create(new CreateOptInCommand(MERCHANT, GRANTEE, Frequency.MONTHLY,
                start, start.plusYears(1)));

        assertThat(created.validUntil()).isEqualTo(start.plusYears(1));

        assertThatThrownBy(() -> service.create(new CreateOptInCommand(OTHER_MERCHANT, GRANTEE,
                Frequency.MONTHLY, start, start.plusYears(1).plusDays(1))))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void startDateAfterValidUntilIsRejected() {                                // R5
        CreateOptInCommand command = new CreateOptInCommand(MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY.plusMonths(6), TODAY.plusMonths(2));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(InvalidValidityException.class);
    }

    @Test
    void revokeStoresRevocationTime() {
        OptIn created = service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(6)));

        OptIn revoked = service.revoke(new RevokeOptInCommand(created.id()));

        assertThat(revoked.status()).isEqualTo(OptInStatus.REVOKED);
        assertThat(revoked.revokedAt()).isEqualTo(NOW);
    }

    @Test
    void revokingTwiceIsRejected() {                                           // R6
        OptIn created = service.create(command(MERCHANT, GRANTEE, TODAY.plusMonths(6)));
        service.revoke(new RevokeOptInCommand(created.id()));

        assertThatThrownBy(() -> service.revoke(new RevokeOptInCommand(created.id())))
                .isInstanceOf(OptInAlreadyRevokedException.class);
    }

    @Test
    void revokingUnknownOptInIsRejected() {                                    // R9
        assertThatThrownBy(() -> service.revoke(new RevokeOptInCommand(UUID.randomUUID())))
                .isInstanceOf(OptInNotFoundException.class);
    }

    @Test
    void dailyCheckRevokesOnlyOptInsWhoseValidUntilHasPassed() {               // R8
        // Seed directly: create() refuses a validUntil in the past.
        OptIn expired = repository.save(new OptIn(null, MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY.minusMonths(2), TODAY.minusDays(1), OptInStatus.ACTIVE, null));
        OptIn lastDay = repository.save(new OptIn(null, MERCHANT, "00000000000191", Frequency.DAILY,
                TODAY.minusMonths(2), TODAY, OptInStatus.ACTIVE, null));
        OptIn future = repository.save(new OptIn(null, OTHER_MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY.minusMonths(2), TODAY.plusDays(1), OptInStatus.ACTIVE, null));

        int revoked = service.revokeExpired();

        assertThat(revoked).isEqualTo(1);
        assertThat(repository.findById(expired.id()).orElseThrow().status()).isEqualTo(OptInStatus.REVOKED);
        assertThat(repository.findById(expired.id()).orElseThrow().revokedAt()).isEqualTo(NOW);
        assertThat(repository.findById(lastDay.id()).orElseThrow().isActive()).isTrue();
        assertThat(repository.findById(future.id()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void dailyCheckIsIdempotent() {
        repository.save(new OptIn(null, MERCHANT, GRANTEE, Frequency.DAILY,
                TODAY.minusMonths(2), TODAY.minusDays(1), OptInStatus.ACTIVE, null));

        assertThat(service.revokeExpired()).isEqualTo(1);
        assertThat(service.revokeExpired()).isEqualTo(0);
    }

    private static CreateOptInCommand command(String merchant, String grantee, LocalDate validUntil) {
        return new CreateOptInCommand(merchant, grantee, Frequency.MONTHLY, TODAY, validUntil);
    }
}
