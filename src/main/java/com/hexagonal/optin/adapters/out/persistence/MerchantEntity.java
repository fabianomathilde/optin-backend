package com.hexagonal.optin.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** JPA entity for the merchant table (V2 migration). Never leaves this package. */
@Entity
@Table(name = "merchant")
class MerchantEntity {

    @Id
    @Column(name = "merchant", length = 14, nullable = false)
    private String merchant;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    protected MerchantEntity() {
        // required by JPA
    }

    MerchantEntity(String merchant, LocalDate createdAt) {
        this.merchant = merchant;
        this.createdAt = createdAt;
    }

    String getMerchant() {
        return merchant;
    }

    LocalDate getCreatedAt() {
        return createdAt;
    }
}
