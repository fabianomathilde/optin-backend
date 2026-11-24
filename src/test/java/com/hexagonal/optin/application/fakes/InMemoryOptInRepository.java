package com.hexagonal.optin.application.fakes;

import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class InMemoryOptInRepository implements OptInRepository {

    private final Map<UUID, OptIn> store = new ConcurrentHashMap<>();

    @Override
    public OptIn save(OptIn optIn) {
        OptIn stored = optIn.id() == null ? optIn.withId(UUID.randomUUID()) : optIn;
        store.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<OptIn> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<OptIn> findActive(String merchant, String grantee) {
        return all().stream()
                .filter(o -> o.isActive() && o.merchant().equals(merchant) && o.grantee().equals(grantee))
                .findFirst();
    }

    @Override
    public List<OptIn> findByGrantee(String grantee, Optional<OptInStatus> status) {
        return filter(o -> o.grantee().equals(grantee), status);
    }

    @Override
    public List<OptIn> findByMerchant(String merchant, Optional<OptInStatus> status) {
        return filter(o -> o.merchant().equals(merchant), status);
    }

    @Override
    public List<OptIn> findActiveValidUntilBefore(LocalDate day) {
        return all().stream()
                .filter(o -> o.isActive() && o.isExpiredOn(day))
                .toList();
    }

    private List<OptIn> filter(Predicate<OptIn> condition, Optional<OptInStatus> status) {
        return all().stream()
                .filter(condition)
                .filter(o -> status.map(s -> o.status() == s).orElse(true))
                .toList();
    }

    private List<OptIn> all() {
        return new ArrayList<>(store.values());
    }
}
