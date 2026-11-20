package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.application.port.in.CreateOptInUseCase;
import com.hexagonal.optin.application.port.in.ListOptInsUseCase;
import com.hexagonal.optin.application.port.in.RevokeOptInUseCase;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import com.hexagonal.optin.domain.Frequency;
import com.hexagonal.optin.domain.InvalidValidityException;
import com.hexagonal.optin.domain.MerchantNotActiveException;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInNotFoundException;
import com.hexagonal.optin.domain.OptInStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OptInController.class)
@WithMockUser
class OptInControllerTest {

    private static final String MERCHANT = "11222333000181";
    private static final String GRANTEE = "00360305000104";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOptInUseCase createOptIn;

    @MockitoBean
    private ListOptInsUseCase listOptIns;

    @MockitoBean
    private RevokeOptInUseCase revokeOptIn;

    @Test
    void createReturns201WithTheCreatedOptIn() throws Exception {
        when(createOptIn.create(any())).thenReturn(activeOptIn(1));

        mockMvc.perform(post("/opt-ins").with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"merchant":"%s","grantee":"%s","frequency":"MONTHLY",
                                 "startDate":"2026-12-01","validUntil":"2027-12-01"}
                                """.formatted(MERCHANT, GRANTEE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.merchant").value(MERCHANT))
                .andExpect(jsonPath("$.grantee").value(GRANTEE))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value("2026-12-01"))
                .andExpect(jsonPath("$.validUntil").value("2027-12-01"));
    }

    @Test
    void listByGranteeReturnsOptIns() throws Exception {
        when(listOptIns.listByGrantee(GRANTEE, Optional.empty()))
                .thenReturn(List.of(activeOptIn(1), activeOptIn(2)));

        mockMvc.perform(get("/grantees/{grantee}/opt-ins", GRANTEE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void listByGranteeFiltersByStatus() throws Exception {
        when(listOptIns.listByGrantee(GRANTEE, Optional.of(OptInStatus.ACTIVE)))
                .thenReturn(List.of(activeOptIn(1)));

        mockMvc.perform(get("/grantees/{grantee}/opt-ins", GRANTEE).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void listByMerchantReturnsEmptyArrayWhenNone() throws Exception {
        when(listOptIns.listByMerchant(MERCHANT, Optional.empty())).thenReturn(List.of());

        mockMvc.perform(get("/merchants/{merchant}/opt-ins", MERCHANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listByMerchantFiltersByStatus() throws Exception {
        when(listOptIns.listByMerchant(MERCHANT, Optional.of(OptInStatus.REVOKED)))
                .thenReturn(List.of());

        mockMvc.perform(get("/merchants/{merchant}/opt-ins", MERCHANT).param("status", "REVOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void revokeReturnsTheRevokedOptIn() throws Exception {
        when(revokeOptIn.revoke(any())).thenReturn(new OptIn(7, MERCHANT, GRANTEE, Frequency.DAILY,
                LocalDate.of(2026, 12, 1), LocalDate.of(2027, 12, 1), OptInStatus.REVOKED,
                Instant.parse("2026-11-19T12:00:00Z")));

        mockMvc.perform(post("/opt-ins/{id}/revoke", 7).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokedAt").value("2026-11-19T12:00:00Z"));
    }

    @Test
    void unknownOptInReturns404WithMessage() throws Exception {
        when(revokeOptIn.revoke(any())).thenThrow(new OptInNotFoundException(99));

        mockMvc.perform(post("/opt-ins/{id}/revoke", 99).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Opt-in 99 not found"));
    }

    @Test
    void duplicateActiveOptInReturns409() throws Exception {
        when(createOptIn.create(any())).thenThrow(new DuplicateActiveOptInException());

        mockMvc.perform(post("/opt-ins").with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"merchant":"%s","grantee":"%s","frequency":"DAILY",
                                 "startDate":"2026-12-01","validUntil":"2027-12-01"}
                                """.formatted(MERCHANT, GRANTEE)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidValidityReturns422() throws Exception {
        when(createOptIn.create(any())).thenThrow(new InvalidValidityException("validUntil must be after today"));

        mockMvc.perform(post("/opt-ins").with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"merchant":"%s","grantee":"%s","frequency":"DAILY",
                                 "startDate":"2026-12-01","validUntil":"2026-11-01"}
                                """.formatted(MERCHANT, GRANTEE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("validUntil must be after today"));
    }

    @Test
    void inactiveMerchantReturns422() throws Exception {
        when(createOptIn.create(any())).thenThrow(new MerchantNotActiveException(MERCHANT));

        mockMvc.perform(post("/opt-ins").with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"merchant":"%s","grantee":"%s","frequency":"DAILY",
                                 "startDate":"2026-12-01","validUntil":"2027-12-01"}
                                """.formatted(MERCHANT, GRANTEE)))
                .andExpect(status().isUnprocessableEntity());
    }

    private static OptIn activeOptIn(int id) {
        return new OptIn(id, MERCHANT, GRANTEE, Frequency.MONTHLY,
                LocalDate.of(2026, 12, 1), LocalDate.of(2027, 12, 1), OptInStatus.ACTIVE, null);
    }
}
