package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.application.port.in.CreateOptInCommand;
import com.hexagonal.optin.application.port.in.CreateOptInUseCase;
import com.hexagonal.optin.application.port.in.ListOptInsUseCase;
import com.hexagonal.optin.application.port.in.RevokeOptInCommand;
import com.hexagonal.optin.application.port.in.RevokeOptInUseCase;
import com.hexagonal.optin.domain.OptInStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Driving REST adapter. Paths and shapes follow src/main/resources/optin-api.yaml. */
@RestController
class OptInController {

    private final CreateOptInUseCase createOptIn;
    private final ListOptInsUseCase listOptIns;
    private final RevokeOptInUseCase revokeOptIn;

    OptInController(CreateOptInUseCase createOptIn,
                    ListOptInsUseCase listOptIns,
                    RevokeOptInUseCase revokeOptIn) {
        this.createOptIn = createOptIn;
        this.listOptIns = listOptIns;
        this.revokeOptIn = revokeOptIn;
    }

    @PostMapping("/opt-ins")
    ResponseEntity<OptInResponse> create(@RequestBody CreateOptInRequest request) {
        var created = createOptIn.create(new CreateOptInCommand(
                request.merchant(),
                request.grantee(),
                request.frequency(),
                request.startDate(),
                request.validUntil()));
        return ResponseEntity.status(HttpStatus.CREATED).body(OptInResponse.from(created));
    }

    @GetMapping("/grantees/{grantee}/opt-ins")
    List<OptInResponse> listByGrantee(@PathVariable String grantee,
                                      @RequestParam(required = false) OptInStatus status) {
        return listOptIns.listByGrantee(grantee, Optional.ofNullable(status))
                .stream().map(OptInResponse::from).toList();
    }

    @GetMapping("/merchants/{merchant}/opt-ins")
    List<OptInResponse> listByMerchant(@PathVariable String merchant,
                                       @RequestParam(required = false) OptInStatus status) {
        return listOptIns.listByMerchant(merchant, Optional.ofNullable(status))
                .stream().map(OptInResponse::from).toList();
    }

    /** A malformed UUID in the path is rejected by Spring with 400 before reaching the use case. */
    @PostMapping("/opt-ins/{id}/revoke")
    OptInResponse revoke(@PathVariable UUID id) {
        return OptInResponse.from(revokeOptIn.revoke(new RevokeOptInCommand(id)));
    }
}
