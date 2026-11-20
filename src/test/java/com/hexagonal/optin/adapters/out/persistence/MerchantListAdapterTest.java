package com.hexagonal.optin.adapters.out.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test with the Spring Data repository mocked. The SQL and the V2 migration are
 * verified by a Testcontainers integration test (CLAUDE.md), which needs a container runtime.
 */
@ExtendWith(MockitoExtension.class)
class MerchantListAdapterTest {

    private static final String MERCHANT = "11222333000181";

    @Mock
    private SpringDataMerchantRepository merchants;

    @InjectMocks
    private MerchantListAdapter adapter;

    @Test
    void merchantInTheTableIsActive() {
        when(merchants.existsById(MERCHANT)).thenReturn(true);

        assertThat(adapter.isActive(MERCHANT)).isTrue();
    }

    @Test
    void merchantNotInTheTableIsNotActive() {
        when(merchants.existsById(MERCHANT)).thenReturn(false);

        assertThat(adapter.isActive(MERCHANT)).isFalse();
    }
}
