package com.hexagonal.optin.adapters.out.persistence;

import com.hexagonal.optin.application.port.out.MerchantList;
import org.springframework.stereotype.Component;

/**
 * Driven adapter for {@link MerchantList} backed by the merchant table.
 * A merchant present in the table is on the list, so it counts as active (R2).
 */
@Component
class MerchantListAdapter implements MerchantList {

    private final SpringDataMerchantRepository merchants;

    MerchantListAdapter(SpringDataMerchantRepository merchants) {
        this.merchants = merchants;
    }

    @Override
    public boolean isActive(String merchant) {
        return merchants.existsById(merchant);
    }
}
