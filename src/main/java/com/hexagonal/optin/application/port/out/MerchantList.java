package com.hexagonal.optin.application.port.out;

import com.hexagonal.optin.domain.Merchant;

/** Driven port: the merchant list (R2). A merchant on the list is active. */
public interface MerchantList {

    boolean isActive(String merchant);

    /** Adds the merchant to the list. Idempotent: an existing merchant is left unchanged. */
    void add(Merchant merchant);
}
