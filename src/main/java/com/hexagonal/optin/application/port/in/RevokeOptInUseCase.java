package com.hexagonal.optin.application.port.in;

import com.hexagonal.optin.domain.OptIn;

/** Driving port: revoke an opt-in. */
public interface RevokeOptInUseCase {

    OptIn revoke(RevokeOptInCommand command);
}
