package com.hexagonal.optin.domain;

/** R2: the merchant is not ACTIVE in the merchant list. */
public class MerchantNotActiveException extends DomainException {

    public MerchantNotActiveException(String merchant) {
        super("Merchant " + merchant + " is not active");
    }
}
