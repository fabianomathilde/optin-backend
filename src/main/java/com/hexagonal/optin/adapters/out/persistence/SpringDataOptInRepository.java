package com.hexagonal.optin.adapters.out.persistence;

import com.hexagonal.optin.domain.OptInStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link OptInEntity}. Used only by the persistence adapter. */
interface SpringDataOptInRepository extends JpaRepository<OptInEntity, UUID> {

    Optional<OptInEntity> findFirstByMerchantAndGranteeAndStatus(String merchant, String grantee, OptInStatus status);

    List<OptInEntity> findByGrantee(String grantee);

    List<OptInEntity> findByGranteeAndStatus(String grantee, OptInStatus status);

    List<OptInEntity> findByMerchant(String merchant);

    List<OptInEntity> findByMerchantAndStatus(String merchant, OptInStatus status);

    List<OptInEntity> findByStatusAndValidUntilBefore(OptInStatus status, LocalDate day);
}
