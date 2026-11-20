package com.hexagonal.optin.application.fakes;

import com.hexagonal.optin.application.port.out.MerchantList;

import java.util.HashSet;
import java.util.Set;

public class InMemoryMerchantList implements MerchantList {

    private final Set<String> active = new HashSet<>();

    public InMemoryMerchantList activate(String merchant) {
        active.add(merchant);
        return this;
    }

    @Override
    public boolean isActive(String merchant) {
        return active.contains(merchant);
    }
}
