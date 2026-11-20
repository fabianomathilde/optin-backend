package com.hexagonal.optin.adapters.out.firestore;

import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.testcontainers.gcloud.FirestoreEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

/** Shared Firestore emulator container for the adapter tests (Testcontainers). */
final class FirestoreEmulator {

    static final DockerImageName IMAGE =
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators");

    private FirestoreEmulator() {
    }

    static FirestoreEmulatorContainer container() {
        return new FirestoreEmulatorContainer(IMAGE);
    }

    static Firestore client(FirestoreEmulatorContainer emulator) {
        return FirestoreOptions.getDefaultInstance().toBuilder()
                .setHost(emulator.getEmulatorEndpoint())
                .setCredentials(NoCredentials.getInstance())
                .setProjectId("test-project")
                .build()
                .getService();
    }
}
