package com.hexagonal.optin.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link MerchantEntity}. Used only by the persistence adapter. */
interface SpringDataMerchantRepository extends JpaRepository<MerchantEntity, String> {
}
