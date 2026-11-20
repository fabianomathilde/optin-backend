package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.application.port.in.CheckOptInValidityUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Driving REST adapter for the daily validity check (R8).
 * Called once a day by an external scheduler. See optin-api.yaml.
 */
@RestController
class OptInValidityController {

    private final CheckOptInValidityUseCase checkValidity;

    OptInValidityController(CheckOptInValidityUseCase checkValidity) {
        this.checkValidity = checkValidity;
    }

    @PostMapping("/opt-ins/validity-checks")
    ValidityCheckResponse check() {
        return new ValidityCheckResponse(checkValidity.revokeExpired());
    }
}
