package com.hexagonal.optin.application.port.in;

/** Driving port: put a merchant on the merchant list (admin operation). */
public interface RegisterMerchantUseCase {

    /** Idempotent: registering a merchant that is already on the list changes nothing. */
    void register(String merchant);
}
