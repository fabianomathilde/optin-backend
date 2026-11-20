package com.hexagonal.optin.application.port.in;

import com.hexagonal.optin.domain.OptIn;

import com.hexagonal.optin.domain.OptInStatus;

import java.util.List;
import java.util.Optional;

/**
 * Driving port: list the opt-ins of a grantee or of a merchant.
 * An empty status means all statuses.
 */
public interface ListOptInsUseCase {

    List<OptIn> listByGrantee(String grantee, Optional<OptInStatus> status);

    List<OptIn> listByMerchant(String merchant, Optional<OptInStatus> status);
}
