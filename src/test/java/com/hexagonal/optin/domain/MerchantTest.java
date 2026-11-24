package com.hexagonal.optin.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Test
    void acceptsFourteenDigits() {
        assertThat(new Merchant("11222333000181", TODAY).document()).isEqualTo("11222333000181");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1122233300018", "112223330001811", "11.222.333/0001-81", "1122233300018a"})
    void rejectsAnythingElse(String document) {
        assertThatThrownBy(() -> new Merchant(document, TODAY))
                .isInstanceOf(InvalidDocumentException.class);
    }
}
