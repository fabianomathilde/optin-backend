package com.hexagonal.optin.application.fakes;

import com.hexagonal.optin.application.port.out.MerchantList;
import com.hexagonal.optin.domain.Merchant;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryMerchantList implements MerchantList {

    private final Map<String, Merchant> merchants = new HashMap<>();

    /** Test shortcut: puts the merchant on the list without going through the use case. */
    public InMemoryMerchantList activate(String merchant) {
        merchants.putIfAbsent(merchant, null);
        return this;
    }

    public Optional<Merchant> find(String merchant) {
        return Optional.ofNullable(merchants.get(merchant));
    }

    @Override
    public boolean isActive(String merchant) {
        return merchants.containsKey(merchant);
    }

    @Override
    public void add(Merchant merchant) {
        merchants.putIfAbsent(merchant.document(), merchant);
    }
}
