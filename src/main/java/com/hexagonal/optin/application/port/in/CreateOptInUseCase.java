package com.hexagonal.optin.application.port.in;

import com.hexagonal.optin.domain.OptIn;

/** Driving port: grant a new opt-in. */
public interface CreateOptInUseCase {

    OptIn create(CreateOptInCommand command);
}
