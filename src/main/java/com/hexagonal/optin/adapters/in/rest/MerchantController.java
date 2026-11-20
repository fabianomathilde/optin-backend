package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.application.port.in.RegisterMerchantUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Driving REST adapter for the merchant list (admin). PUT is idempotent: calling it again for a
 * merchant already on the list changes nothing. See optin-api.yaml.
 */
@RestController
class MerchantController {

    private final RegisterMerchantUseCase registerMerchant;

    MerchantController(RegisterMerchantUseCase registerMerchant) {
        this.registerMerchant = registerMerchant;
    }

    @PutMapping("/merchants/{merchant}")
    ResponseEntity<Void> register(@PathVariable String merchant) {
        registerMerchant.register(merchant);
        return ResponseEntity.noContent().build();
    }
}
