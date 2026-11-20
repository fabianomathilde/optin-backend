package com.hexagonal.optin.application.fakes;

import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.domain.OptIn;
import com.hexagonal.optin.domain.OptInStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryOptInRepository implements OptInRepository {

    private final Map<Integer, OptIn> store = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public OptIn save(OptIn optIn) {
        int id = optIn.id() == null ? sequence.getAndIncrement() : optIn.id();
        OptIn stored = new OptIn(id, optIn.merchant(), optIn.grantee(), optIn.frequency(),
                optIn.startDate(), optIn.validUntil(), optIn.status(), optIn.revokedAt());
        store.put(id, stored);
        return stored;
    }

    @Override
    public Optional<OptIn> findById(int id) {
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

    private List<OptIn> filter(java.util.function.Predicate<OptIn> condition, Optional<OptInStatus> status) {
        return all().stream()
                .filter(condition)
                .filter(o -> status.map(s -> o.status() == s).orElse(true))
                .toList();
    }

    private List<OptIn> all() {
        List<OptIn> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(OptIn::id));
        return list;
    }
}
