package com.hexagonal.optin.adapters.out.persistence;

import com.hexagonal.optin.domain.Frequency;
import com.hexagonal.optin.domain.OptInStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** JPA entity for the opt_in table (V1 migration). Never leaves this package. */
@Entity
@Table(name = "opt_in")
class OptInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "opt_in_id_gen")
    @SequenceGenerator(name = "opt_in_id_gen", sequenceName = "opt_in_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "merchant", length = 14, nullable = false)
    private String merchant;

    @Column(name = "grantee", length = 14, nullable = false)
    private String grantee;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 10, nullable = false)
    private Frequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private OptInStatus status;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected OptInEntity() {
        // required by JPA
    }

    OptInEntity(Integer id, String merchant, String grantee, Frequency frequency,
                LocalDate startDate, LocalDate validUntil, OptInStatus status, Instant revokedAt) {
        this.id = id;
        this.merchant = merchant;
        this.grantee = grantee;
        this.frequency = frequency;
        this.startDate = startDate;
        this.validUntil = validUntil;
        this.status = status;
        this.revokedAt = revokedAt;
    }

    Integer getId() {
        return id;
    }

    String getMerchant() {
        return merchant;
    }

    String getGrantee() {
        return grantee;
    }

    Frequency getFrequency() {
        return frequency;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getValidUntil() {
        return validUntil;
    }

    OptInStatus getStatus() {
        return status;
    }

    Instant getRevokedAt() {
        return revokedAt;
    }
}
