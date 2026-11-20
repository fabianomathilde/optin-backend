package com.hexagonal.optin.adapters.out.persistence;

import com.hexagonal.optin.application.port.out.MerchantList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the merchant adapter and the V2 migration against PostgreSQL (Testcontainers). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MerchantListAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class MerchantListPersistenceTest {

    private static final String MERCHANT = "11222333000181";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MerchantList merchants;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void merchantInTheTableIsActive() {
        jdbc.update("INSERT INTO merchant (merchant, created_at) VALUES (?, ?)",
                MERCHANT, LocalDate.of(2026, 11, 1));

        assertThat(merchants.isActive(MERCHANT)).isTrue();
    }

    @Test
    void merchantNotInTheTableIsNotActive() {
        assertThat(merchants.isActive("99999999000199")).isFalse();
    }
}
