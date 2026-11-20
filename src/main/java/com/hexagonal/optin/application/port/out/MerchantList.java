package com.hexagonal.optin.application.port.out;

/** Driven port: the merchant list (R2). The adapter is defined when the merchant table is provided. */
public interface MerchantList {

    boolean isActive(String merchant);
}
