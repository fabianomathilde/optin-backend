package com.hexagonal.optin.adapters.out.firestore;

import com.google.cloud.firestore.Firestore;
import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.application.port.out.OptInRepositoryContractTest;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import com.hexagonal.optin.domain.OptIn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.gcloud.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the OptInRepository contract against the Firestore emulator, plus the R1 guarantees that
 * the Postgres adapter gets from its partial unique index. Skipped without a container runtime.
 */
@Testcontainers(disabledWithoutDocker = true)
class FirestoreOptInRepositoryTest extends OptInRepositoryContractTest {

    @Container
    static FirestoreEmulatorContainer emulator = FirestoreEmulator.container();

    private static Firestore firestore;
    private static OptInRepository repository;

    @BeforeAll
    static void connect() {
        firestore = FirestoreEmulator.client(emulator);
        repository = new FirestoreOptInRepository(firestore);
    }

    @AfterAll
    static void disconnect() throws Exception {
        firestore.close();
    }

    @Override
    protected OptInRepository repository() {
        return repository;
    }

    @Test
    void secondActiveOptInForTheSamePairIsRejected() {                          // R1 backstop
        String merchant = newDocument();
        String grantee = newDocument();
        persist(draft(merchant, grantee, LocalDate.of(2027, 11, 1)));

        assertThatThrownBy(() -> persist(draft(merchant, grantee, LocalDate.of(2027, 12, 1))))
                .isInstanceOf(DuplicateActiveOptInException.class);
    }

    @Test
    void pairCanBeGrantedAgainAfterRevocation() {                              // guard is released
        String merchant = newDocument();
        String grantee = newDocument();
        OptIn first = persist(draft(merchant, grantee, LocalDate.of(2027, 11, 1)));
        persist(first.revoke(Instant.parse("2026-11-20T12:00:00Z")));

        OptIn second = persist(draft(merchant, grantee, LocalDate.of(2027, 12, 1)));

        assertThat(repository.findActive(merchant, grantee)).contains(second);
    }

    @Test
    void concurrentGrantsForTheSamePairCommitOnlyOnce() throws Exception {      // R1 under a race
        String merchant = newDocument();
        String grantee = newDocument();
        Callable<Boolean> grant = () -> {
            try {
                persist(draft(merchant, grantee, LocalDate.of(2027, 11, 1)));
                return true;
            } catch (DuplicateActiveOptInException e) {
                return false;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(grant));
            }
            long committed = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    committed++;
                }
            }
            assertThat(committed).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
