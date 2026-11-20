package com.hexagonal.optin.application.service;

import com.hexagonal.optin.application.port.in.RegisterMerchantUseCase;
import com.hexagonal.optin.application.port.out.MerchantList;
import com.hexagonal.optin.domain.Merchant;

import java.time.Clock;
import java.time.LocalDate;

/** Use case implementation for the merchant list. The format rule lives in {@link Merchant}. */
public class MerchantService implements RegisterMerchantUseCase {

    private final MerchantList merchants;
    private final Clock clock;

    public MerchantService(MerchantList merchants, Clock clock) {
        this.merchants = merchants;
        this.clock = clock;
    }

    @Override
    public void register(String merchant) {
        merchants.add(new Merchant(merchant, LocalDate.now(clock)));
    }
}
