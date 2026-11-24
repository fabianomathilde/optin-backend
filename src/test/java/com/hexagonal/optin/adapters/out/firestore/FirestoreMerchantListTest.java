package com.hexagonal.optin.adapters.out.firestore;

import com.google.cloud.firestore.Firestore;
import com.hexagonal.optin.domain.Merchant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.gcloud.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Merchant list adapter against the Firestore emulator. Skipped without a container runtime. */
@Testcontainers(disabledWithoutDocker = true)
class FirestoreMerchantListTest {

    @Container
    static FirestoreEmulatorContainer emulator = FirestoreEmulator.container();

    private static Firestore firestore;
    private static FirestoreMerchantList merchants;

    @BeforeAll
    static void connect() {
        firestore = FirestoreEmulator.client(emulator);
        merchants = new FirestoreMerchantList(firestore);
    }

    @AfterAll
    static void disconnect() throws Exception {
        firestore.close();
    }

    @Test
    void addedMerchantIsActive() {
        merchants.add(new Merchant("11222333000181", LocalDate.of(2026, 11, 20)));

        assertThat(merchants.isActive("11222333000181")).isTrue();
    }

    @Test
    void unknownMerchantIsNotActive() {
        assertThat(merchants.isActive("99999999000199")).isFalse();
    }

    @Test
    void addingTwiceKeepsTheFirstRegistration() throws Exception {
        merchants.add(new Merchant("45543915000181", LocalDate.of(2026, 11, 20)));
        merchants.add(new Merchant("45543915000181", LocalDate.of(2026, 12, 1)));

        String registeredOn = firestore.collection(FirestoreMerchantList.MERCHANTS)
                .document("45543915000181").get().get().getString("registeredOn");
        assertThat(registeredOn).isEqualTo("2026-11-20");
    }
}
