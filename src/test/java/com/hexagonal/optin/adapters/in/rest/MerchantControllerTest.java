package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.application.port.in.RegisterMerchantUseCase;
import com.hexagonal.optin.domain.InvalidDocumentException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MerchantController.class)
@WithMockUser
class MerchantControllerTest {

    private static final String MERCHANT = "11222333000181";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterMerchantUseCase registerMerchant;

    @Test
    void registerReturns204() throws Exception {
        mockMvc.perform(put("/merchants/{merchant}", MERCHANT).with(csrf()))
                .andExpect(status().isNoContent());

        verify(registerMerchant).register(MERCHANT);
    }

    @Test
    void invalidDocumentReturns422() throws Exception {
        doThrow(new InvalidDocumentException("123")).when(registerMerchant).register("123");

        mockMvc.perform(put("/merchants/{merchant}", "123").with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Document 123 is not a valid CNPJ (14 digits)"));
    }
}
