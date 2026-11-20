package com.hexagonal.optin.adapters.out.firestore;

import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the Firestore client for the "firestore" profile (ADR 0005). Lives in the adapter
 * package so that no Firestore type leaks into config/ or the core.
 *
 * <ul>
 *   <li>GKE: project from {@code optin.firestore.project-id}; credentials from Workload Identity
 *       (Application Default Credentials).</li>
 *   <li>Local / tests: {@code optin.firestore.emulator-host} points to the emulator
 *       (no credentials needed).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "optin.persistence", havingValue = "firestore")
class FirestoreClientConfiguration {

    @Bean(destroyMethod = "close")
    Firestore firestore(@Value("${optin.firestore.project-id:}") String projectId,
                        @Value("${optin.firestore.database-id:(default)}") String databaseId,
                        @Value("${optin.firestore.emulator-host:}") String emulatorHost) {
        FirestoreOptions.Builder options = FirestoreOptions.getDefaultInstance().toBuilder()
                .setDatabaseId(databaseId);
        if (!projectId.isBlank()) {
            options.setProjectId(projectId);
        }
        if (!emulatorHost.isBlank()) {
            options.setHost(emulatorHost).setCredentials(NoCredentials.getInstance());
            if (projectId.isBlank()) {
                options.setProjectId("demo-optin");
            }
        }
        return options.build().getService();
    }
}
