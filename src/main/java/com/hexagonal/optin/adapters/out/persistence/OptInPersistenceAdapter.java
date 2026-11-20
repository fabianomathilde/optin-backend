package com.hexagonal.optin.adapters.out.persistence;

import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Driven adapter for {@link OptInRepository} backed by PostgreSQL (phase 2).
 * Converts between the domain {@link OptIn} and {@link OptInEntity}.
 */
@Component
class OptInPersistenceAdapter implements OptInRepository {

    /** Partial unique index from V1 that backs R1 when two grants race. */
    static final String ACTIVE_PAIR_INDEX = "uq_opt_in_active_pair";

    private final SpringDataOptInRepository jpa;

    OptInPersistenceAdapter(SpringDataOptInRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OptIn save(OptIn optIn) {
        try {
            return toDomain(jpa.saveAndFlush(toEntity(optIn)));
        } catch (DataIntegrityViolationException e) {
            if (violatesActivePairIndex(e)) {
                throw new DuplicateActiveOptInException();
            }
            throw e;
        }
    }

    @Override
    public Optional<OptIn> findById(int id) {
        return jpa.findById(id).map(OptInPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<OptIn> findActive(String merchant, String grantee) {
        return jpa.findFirstByMerchantAndGranteeAndStatus(merchant, grantee, OptInStatus.ACTIVE)
                .map(OptInPersistenceAdapter::toDomain);
    }

    @Override
    public List<OptIn> findByGrantee(String grantee, Optional<OptInStatus> status) {
        List<OptInEntity> rows = status
                .map(s -> jpa.findByGranteeAndStatus(grantee, s))
                .orElseGet(() -> jpa.findByGrantee(grantee));
        return toDomain(rows);
    }

    @Override
    public List<OptIn> findByMerchant(String merchant, Optional<OptInStatus> status) {
        List<OptInEntity> rows = status
                .map(s -> jpa.findByMerchantAndStatus(merchant, s))
                .orElseGet(() -> jpa.findByMerchant(merchant));
        return toDomain(rows);
    }

    @Override
    public List<OptIn> findActiveValidUntilBefore(LocalDate day) {
        return toDomain(jpa.findByStatusAndValidUntilBefore(OptInStatus.ACTIVE, day));
    }

    private static boolean violatesActivePairIndex(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause.getMessage() != null ? cause.getMessage() : e.getMessage();
        return message != null && message.contains(ACTIVE_PAIR_INDEX);
    }

    private static OptInEntity toEntity(OptIn o) {
        return new OptInEntity(o.id(), o.merchant(), o.grantee(), o.frequency(),
                o.startDate(), o.validUntil(), o.status(), o.revokedAt());
    }

    private static OptIn toDomain(OptInEntity e) {
        return new OptIn(e.getId(), e.getMerchant(), e.getGrantee(), e.getFrequency(),
                e.getStartDate(), e.getValidUntil(), e.getStatus(), e.getRevokedAt());
    }

    private static List<OptIn> toDomain(List<OptInEntity> rows) {
        return rows.stream().map(OptInPersistenceAdapter::toDomain).toList();
    }
}
