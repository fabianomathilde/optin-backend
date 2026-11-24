package com.hexagonal.optin.application.port.in;

import java.util.UUID;

public record RevokeOptInCommand(UUID optInId) {
}
