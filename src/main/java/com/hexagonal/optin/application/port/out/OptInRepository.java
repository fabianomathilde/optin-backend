package com.hexagonal.optin.application.port.out;

import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Driven port: storage of opt-ins. Finders use domain language, not JPA names (CLAUDE.md). */
public interface OptInRepository {

    /** Inserts when {@code id} is null (the repository assigns it), otherwise updates. */
    OptIn save(OptIn optIn);

    Optional<OptIn> findById(int id);

    Optional<OptIn> findActive(String merchant, String grantee);

    List<OptIn> findByGrantee(String grantee, Optional<OptInStatus> status);

    List<OptIn> findByMerchant(String merchant, Optional<OptInStatus> status);

    /** ACTIVE opt-ins whose validUntil is before the given day. */
    List<OptIn> findActiveValidUntilBefore(LocalDate day);
}
