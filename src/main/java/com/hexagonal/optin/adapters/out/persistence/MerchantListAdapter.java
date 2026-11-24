package com.hexagonal.optin.adapters.out.persistence;

import com.hexagonal.optin.application.port.out.MerchantList;
import com.hexagonal.optin.domain.Merchant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Driven adapter for {@link MerchantList} backed by the merchant table.
 * A merchant present in the table is on the list, so it counts as active (R2).
 */
@Component
@ConditionalOnProperty(name = "optin.persistence", havingValue = "postgres", matchIfMissing = true)
class MerchantListAdapter implements MerchantList {

    private final SpringDataMerchantRepository merchants;

    MerchantListAdapter(SpringDataMerchantRepository merchants) {
        this.merchants = merchants;
    }

    @Override
    public boolean isActive(String merchant) {
        return merchants.existsById(merchant);
    }

    @Override
    public void add(Merchant merchant) {
        if (!merchants.existsById(merchant.document())) {
            merchants.save(new MerchantEntity(merchant.document(), merchant.registeredOn()));
        }
    }
}
