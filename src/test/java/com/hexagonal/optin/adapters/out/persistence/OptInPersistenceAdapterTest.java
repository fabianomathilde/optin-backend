package com.hexagonal.optin.adapters.out.persistence;

import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.application.port.out.OptInRepositoryContractTest;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the OptInRepository contract against PostgreSQL (Testcontainers) with the V1 migration.
 * Skipped when no container runtime is available (Docker or Podman with the Docker socket).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OptInPersistenceAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class OptInPersistenceAdapterTest extends OptInRepositoryContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private OptInRepository repository;

    @Override
    protected OptInRepository repository() {
        return repository;
    }

    @Test
    void secondActiveOptInForTheSamePairIsRejectedByTheDatabaseIndex() {          // R1 backstop
        String merchant = newDocument();
        String grantee = newDocument();
        persist(draft(merchant, grantee, LocalDate.of(2027, 11, 1)));

        assertThatThrownBy(() -> persist(draft(merchant, grantee, LocalDate.of(2027, 12, 1))))
                .isInstanceOf(DuplicateActiveOptInException.class);
    }
}
