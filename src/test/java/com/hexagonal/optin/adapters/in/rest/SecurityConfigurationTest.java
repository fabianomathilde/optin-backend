package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.application.port.in.CheckOptInValidityUseCase;
import com.hexagonal.optin.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API security: Basic auth works for POST without a CSRF token, no session cookie is issued, and health is public. */
@WebMvcTest(value = OptInValidityController.class, properties = "spring.security.user.password=secret")
@Import(SecurityConfiguration.class)
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckOptInValidityUseCase checkValidity;

    @Test
    void postWithBasicAuthAndNoCsrfTokenIsAccepted() throws Exception {
        when(checkValidity.revokeExpired()).thenReturn(0);

        mockMvc.perform(post("/opt-ins/validity-checks").with(httpBasic("user", "secret")))
                .andExpect(status().isOk());
    }

    @Test
    void postWithoutCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(post("/opt-ins/validity-checks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noSessionCookieIsIssued() throws Exception {
        when(checkValidity.revokeExpired()).thenReturn(0);

        var result = mockMvc.perform(post("/opt-ins/validity-checks").with(httpBasic("user", "secret")))
                .andReturn();

        assertThat(result.getResponse().getCookie("JSESSIONID")).isNull();
    }

    @Test
    void healthEndpointDoesNotRequireCredentials() throws Exception {
        // Actuator isn't part of this @WebMvcTest slice, so the path resolves to 404; the point is
        // that security lets it through instead of answering 401.
        var result = mockMvc.perform(get("/actuator/health/readiness")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }
}
