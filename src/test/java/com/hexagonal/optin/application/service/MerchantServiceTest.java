package com.hexagonal.optin.application.service;

import com.hexagonal.optin.application.fakes.InMemoryMerchantList;
import com.hexagonal.optin.domain.InvalidDocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantServiceTest {

    private static final String MERCHANT = "11222333000181";
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    private InMemoryMerchantList merchants;
    private MerchantService service;

    @BeforeEach
    void setUp() {
        merchants = new InMemoryMerchantList();
        service = new MerchantService(merchants, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registeredMerchantBecomesActiveWithTodaysDate() {
        service.register(MERCHANT);

        assertThat(merchants.isActive(MERCHANT)).isTrue();
        assertThat(merchants.find(MERCHANT).orElseThrow().registeredOn()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void registeringTwiceKeepsTheOriginalDate() {
        service.register(MERCHANT);
        MerchantService later = new MerchantService(merchants,
                Clock.fixed(NOW.plusSeconds(86_400), ZoneOffset.UTC));

        later.register(MERCHANT);

        assertThat(merchants.find(MERCHANT).orElseThrow().registeredOn()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void invalidDocumentIsRejected() {
        assertThatThrownBy(() -> service.register("123"))
                .isInstanceOf(InvalidDocumentException.class);

        assertThat(merchants.isActive("123")).isFalse();
    }
}
