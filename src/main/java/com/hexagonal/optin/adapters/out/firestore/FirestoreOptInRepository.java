package com.hexagonal.optin.adapters.out.firestore;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import com.hexagonal.optin.domain.Frequency;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.hexagonal.optin.adapters.out.firestore.Futures.await;

/**
 * Driven adapter for {@link OptInRepository} backed by Firestore (ADR 0005).
 *
 * <pre>
 * optIns/{uuid}                       the opt-in (dates as ISO strings so they sort correctly)
 * activeOptIns/{merchant}_{grantee}   R1 guard: exists only while the pair has an ACTIVE opt-in
 * </pre>
 *
 * Every save runs in one transaction that reads the guard first, then writes the opt-in and
 * creates or deletes the guard. Two concurrent grants for the same pair cannot both commit: the
 * second sees the guard and fails with {@link DuplicateActiveOptInException}, like the partial
 * unique index in the Postgres adapter.
 */
@Component
@ConditionalOnProperty(name = "optin.persistence", havingValue = "firestore")
class FirestoreOptInRepository implements OptInRepository {

    static final String OPT_INS = "optIns";
    static final String ACTIVE_GUARDS = "activeOptIns";

    private final Firestore db;

    FirestoreOptInRepository(Firestore db) {
        this.db = db;
    }

    @Override
    public OptIn save(OptIn optIn) {
        OptIn toStore = optIn.id() == null ? optIn.withId(UUID.randomUUID()) : optIn;
        String id = toStore.id().toString();
        DocumentReference document = db.collection(OPT_INS).document(id);
        DocumentReference guard = guardFor(toStore.merchant(), toStore.grantee());

        await(db.runTransaction(tx -> {
            DocumentSnapshot current = tx.get(guard).get();               // reads before writes
            boolean guardIsMine = current.exists() && id.equals(current.getString("optInId"));
            if (toStore.isActive()) {
                if (current.exists() && !guardIsMine) {
                    throw new DuplicateActiveOptInException();            // R1
                }
                tx.set(guard, Map.of("optInId", id));
            } else if (guardIsMine) {
                tx.delete(guard);                                         // pair is free again
            }
            tx.set(document, toFields(toStore));
            return null;
        }));
        return toStore;
    }

    @Override
    public Optional<OptIn> findById(UUID id) {
        DocumentSnapshot snapshot = await(db.collection(OPT_INS).document(id.toString()).get());
        return snapshot.exists() ? Optional.of(toDomain(snapshot)) : Optional.empty();
    }

    @Override
    public Optional<OptIn> findActive(String merchant, String grantee) {
        DocumentSnapshot guard = await(guardFor(merchant, grantee).get());
        if (!guard.exists()) {
            return Optional.empty();
        }
        return findById(UUID.fromString(guard.getString("optInId")))
                .filter(OptIn::isActive);
    }

    @Override
    public List<OptIn> findByGrantee(String grantee, Optional<OptInStatus> status) {
        return run(withStatus(db.collection(OPT_INS).whereEqualTo("grantee", grantee), status));
    }

    @Override
    public List<OptIn> findByMerchant(String merchant, Optional<OptInStatus> status) {
        return run(withStatus(db.collection(OPT_INS).whereEqualTo("merchant", merchant), status));
    }

    /** Needs the composite index (status ASC, validUntil ASC), created by the IaC repo. */
    @Override
    public List<OptIn> findActiveValidUntilBefore(LocalDate day) {
        return run(db.collection(OPT_INS)
                .whereEqualTo("status", OptInStatus.ACTIVE.name())
                .whereLessThan("validUntil", day.toString()));
    }

    private DocumentReference guardFor(String merchant, String grantee) {
        return db.collection(ACTIVE_GUARDS).document(merchant + "_" + grantee);
    }

    private static Query withStatus(Query query, Optional<OptInStatus> status) {
        return status.map(s -> query.whereEqualTo("status", s.name())).orElse(query);
    }

    private List<OptIn> run(Query query) {
        return await(query.get()).getDocuments().stream()
                .map(FirestoreOptInRepository::toDomain)
                .toList();
    }

    private static Map<String, Object> toFields(OptIn o) {
        Map<String, Object> fields = new HashMap<>();                     // allows null values
        fields.put("merchant", o.merchant());
        fields.put("grantee", o.grantee());
        fields.put("frequency", o.frequency().name());
        fields.put("startDate", o.startDate().toString());
        fields.put("validUntil", o.validUntil() == null ? null : o.validUntil().toString());
        fields.put("status", o.status().name());
        fields.put("revokedAt", o.revokedAt() == null ? null
                : Timestamp.ofTimeSecondsAndNanos(o.revokedAt().getEpochSecond(), o.revokedAt().getNano()));
        return fields;
    }

    private static OptIn toDomain(DocumentSnapshot d) {
        String validUntil = d.getString("validUntil");
        Timestamp revokedAt = d.getTimestamp("revokedAt");
        return new OptIn(
                UUID.fromString(d.getId()),
                d.getString("merchant"),
                d.getString("grantee"),
                Frequency.valueOf(d.getString("frequency")),
                LocalDate.parse(d.getString("startDate")),
                validUntil == null ? null : LocalDate.parse(validUntil),
                OptInStatus.valueOf(d.getString("status")),
                revokedAt == null ? null : Instant.ofEpochSecond(revokedAt.getSeconds(), revokedAt.getNanos()));
    }
}
