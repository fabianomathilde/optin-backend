package com.hexagonal.optin;

import com.hexagonal.optin.application.port.out.MerchantList;
import com.hexagonal.optin.application.port.out.OptInRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.gcloud.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the whole application with the "firestore" profile against the emulator: proves the
 * Firestore adapters are wired and that no DataSource / JPA / Flyway is started (ADR 0005).
 */
@SpringBootTest
@ActiveProfiles("firestore")
@Testcontainers(disabledWithoutDocker = true)
class FirestoreProfileApplicationTests {

    @Container
    static FirestoreEmulatorContainer emulator = new FirestoreEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators"));

    @DynamicPropertySource
    static void firestore(DynamicPropertyRegistry registry) {
        registry.add("optin.firestore.emulator-host", emulator::getEmulatorEndpoint);
        registry.add("optin.firestore.project-id", () -> "test-project");
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private OptInRepository optIns;

    @Autowired
    private MerchantList merchants;

    @Test
    void firestoreAdaptersAreWiredAndNoDataSourceExists() {
        assertThat(optIns.getClass().getSimpleName()).startsWith("Firestore");
        assertThat(merchants.getClass().getSimpleName()).startsWith("Firestore");
        assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
    }
}
