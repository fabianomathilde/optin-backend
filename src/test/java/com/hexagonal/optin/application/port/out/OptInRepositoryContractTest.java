package com.hexagonal.optin.application.port.out;

import com.hexagonal.optin.domain.Frequency;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract for {@link OptInRepository}. Every implementation (in-memory fake, PostgreSQL adapter)
 * extends this class and supplies its repository, so both must behave the same way.
 * Each test uses fresh document numbers, so implementations that keep data between tests are fine.
 */
public abstract class OptInRepositoryContractTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final AtomicInteger DOCUMENTS = new AtomicInteger(1);

    protected abstract OptInRepository repository();

    protected static String newDocument() {
        return String.format("%014d", DOCUMENTS.getAndIncrement());
    }

    protected static OptIn draft(String merchant, String grantee, LocalDate validUntil) {
        return new OptIn(null, merchant, grantee, Frequency.MONTHLY, START, validUntil,
                OptInStatus.ACTIVE, null);
    }

    protected OptIn persist(OptIn optIn) {
        return repository().save(optIn);
    }

    @Test
    public void saveAssignsIdAndReturnsTheStoredOptIn() {
        String merchant = newDocument();
        String grantee = newDocument();

        OptIn saved = persist(draft(merchant, grantee, LocalDate.of(2027, 11, 1)));

        assertThat(saved.id()).isNotNull();                                    // a UUID assigned by the repository
        assertThat(saved.merchant()).isEqualTo(merchant);
        assertThat(saved.grantee()).isEqualTo(grantee);
        assertThat(saved.frequency()).isEqualTo(Frequency.MONTHLY);
        assertThat(saved.startDate()).isEqualTo(START);
        assertThat(saved.validUntil()).isEqualTo(LocalDate.of(2027, 11, 1));
        assertThat(saved.status()).isEqualTo(OptInStatus.ACTIVE);
        assertThat(saved.revokedAt()).isNull();
        assertThat(repository().findById(saved.id())).isEqualTo(java.util.Optional.of(saved));
    }

    @Test
    public void findByIdIsEmptyForUnknownId() {
        assertThat(repository().findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    public void saveOfAnExistingOptInUpdatesIt() {
        OptIn saved = persist(draft(newDocument(), newDocument(), LocalDate.of(2027, 11, 1)));

        persist(saved.revoke(NOW));

        OptIn reloaded = repository().findById(saved.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(OptInStatus.REVOKED);
        assertThat(reloaded.revokedAt()).isEqualTo(NOW);
    }

    @Test
    public void findActiveReturnsOnlyTheActiveOptInOfThePair() {
        String merchant = newDocument();
        String grantee = newDocument();
        OptIn active = persist(draft(merchant, grantee, LocalDate.of(2027, 11, 1)));
        persist(draft(merchant, newDocument(), LocalDate.of(2027, 11, 1)));

        assertThat(repository().findActive(merchant, grantee)).contains(active);
    }

    @Test
    public void findActiveIsEmptyOnceRevoked() {
        String merchant = newDocument();
        String grantee = newDocument();
        OptIn active = persist(draft(merchant, grantee, LocalDate.of(2027, 11, 1)));
        persist(active.revoke(NOW));

        assertThat(repository().findActive(merchant, grantee)).isEmpty();
    }

    @Test
    public void findByGranteeFiltersByStatus() {
        String grantee = newDocument();
        OptIn first = persist(draft(newDocument(), grantee, LocalDate.of(2027, 11, 1)));
        OptIn second = persist(draft(newDocument(), grantee, LocalDate.of(2027, 11, 1)));
        OptIn revoked = persist(second.revoke(NOW));

        assertThat(repository().findByGrantee(grantee, java.util.Optional.empty())).hasSize(2);
        assertThat(repository().findByGrantee(grantee, java.util.Optional.of(OptInStatus.ACTIVE)))
                .containsExactly(first);
        assertThat(repository().findByGrantee(grantee, java.util.Optional.of(OptInStatus.REVOKED)))
                .containsExactly(revoked);
    }

    @Test
    public void findByMerchantFiltersByStatus() {
        String merchant = newDocument();
        OptIn first = persist(draft(merchant, newDocument(), LocalDate.of(2027, 11, 1)));
        OptIn second = persist(draft(merchant, newDocument(), LocalDate.of(2027, 11, 1)));
        OptIn revoked = persist(second.revoke(NOW));

        assertThat(repository().findByMerchant(merchant, java.util.Optional.empty())).hasSize(2);
        assertThat(repository().findByMerchant(merchant, java.util.Optional.of(OptInStatus.ACTIVE)))
                .containsExactly(first);
        assertThat(repository().findByMerchant(merchant, java.util.Optional.of(OptInStatus.REVOKED)))
                .containsExactly(revoked);
    }

    @Test
    public void findActiveValidUntilBeforeReturnsExpiredActiveOptIns() {
        String merchant = newDocument();
        OptIn expired = persist(draft(merchant, newDocument(), LocalDate.of(2026, 11, 18)));
        persist(draft(merchant, newDocument(), LocalDate.of(2026, 11, 19)));        // still effective
        persist(draft(merchant, newDocument(), LocalDate.of(2026, 11, 20)));        // future
        persist(persist(draft(merchant, newDocument(), LocalDate.of(2026, 11, 10))).revoke(NOW));

        var found = repository().findActiveValidUntilBefore(LocalDate.of(2026, 11, 19)).stream()
                .filter(o -> o.merchant().equals(merchant))
                .toList();

        assertThat(found).containsExactly(expired);
    }
}
