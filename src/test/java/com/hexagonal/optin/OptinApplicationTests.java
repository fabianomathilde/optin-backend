package com.hexagonal.optin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Loads the whole application against PostgreSQL. Skipped when no container runtime is available. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OptinApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void contextLoads() {
	}

}
