package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.application.port.in.CheckOptInValidityUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OptInValidityController.class)
@WithMockUser
class OptInValidityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckOptInValidityUseCase checkValidity;

    @Test
    void validityCheckReturnsRevokedCount() throws Exception {
        when(checkValidity.revokeExpired()).thenReturn(2);

        mockMvc.perform(post("/opt-ins/validity-checks").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revokedCount").value(2));
    }
}
