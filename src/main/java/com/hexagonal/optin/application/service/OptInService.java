package com.hexagonal.optin.application.service;

import com.hexagonal.optin.application.port.in.CheckOptInValidityUseCase;
import com.hexagonal.optin.application.port.in.CreateOptInCommand;
import com.hexagonal.optin.application.port.in.CreateOptInUseCase;
import com.hexagonal.optin.application.port.in.ListOptInsUseCase;
import com.hexagonal.optin.application.port.in.RevokeOptInCommand;
import com.hexagonal.optin.application.port.in.RevokeOptInUseCase;
import com.hexagonal.optin.application.port.out.MerchantList;
import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import com.hexagonal.optin.domain.MerchantNotActiveException;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInNotFoundException;
import com.hexagonal.optin.domain.OptInStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Use case implementation: orchestrates the repository, the merchant list and the clock.
 * The rules themselves live in {@link OptIn} (see ADR 0003).
 */
public class OptInService implements CreateOptInUseCase, ListOptInsUseCase,
        RevokeOptInUseCase, CheckOptInValidityUseCase {

    private final OptInRepository repository;
    private final MerchantList merchants;
    private final Clock clock;

    public OptInService(OptInRepository repository, MerchantList merchants, Clock clock) {
        this.repository = repository;
        this.merchants = merchants;
        this.clock = clock;
    }

    @Override
    public OptIn create(CreateOptInCommand command) {
        LocalDate today = LocalDate.now(clock);
        OptIn candidate = OptIn.grant(command.merchant(), command.grantee(), command.frequency(),
                command.startDate(), command.validUntil(), today);                  // R3, R4, R5

        if (repository.findActive(command.merchant(), command.grantee()).isPresent()) {
            throw new DuplicateActiveOptInException();                              // R1
        }
        if (!merchants.isActive(command.merchant())) {
            throw new MerchantNotActiveException(command.merchant());               // R2
        }
        return repository.save(candidate);
    }

    @Override
    public List<OptIn> listByGrantee(String grantee, Optional<OptInStatus> status) {
        return repository.findByGrantee(grantee, status);
    }

    @Override
    public List<OptIn> listByMerchant(String merchant, Optional<OptInStatus> status) {
        return repository.findByMerchant(merchant, status);
    }

    @Override
    public OptIn revoke(RevokeOptInCommand command) {
        OptIn optIn = repository.findById(command.optInId())
                .orElseThrow(() -> new OptInNotFoundException(command.optInId()));  // R9
        return repository.save(optIn.revoke(Instant.now(clock)));                   // R6
    }

    @Override
    public int revokeExpired() {
        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);
        List<OptIn> expired = repository.findActiveValidUntilBefore(today);         // R8
        for (OptIn optIn : expired) {
            repository.save(optIn.revoke(now));
        }
        return expired.size();
    }
}
