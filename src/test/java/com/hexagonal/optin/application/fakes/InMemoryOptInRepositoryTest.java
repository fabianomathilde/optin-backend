package com.hexagonal.optin.application.fakes;

import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.application.port.out.OptInRepositoryContractTest;

/** Runs the OptInRepository contract against the in-memory fake. */
class InMemoryOptInRepositoryTest extends OptInRepositoryContractTest {

    private final OptInRepository repository = new InMemoryOptInRepository();

    @Override
    protected OptInRepository repository() {
        return repository;
    }
}
