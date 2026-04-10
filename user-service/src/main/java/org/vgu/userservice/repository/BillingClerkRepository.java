package org.vgu.userservice.repository;

import org.vgu.userservice.model.BillingClerk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingClerkRepository extends JpaRepository<BillingClerk, Long> {
    
    Optional<BillingClerk> findByUser_KeycloakUserId(String keycloakUserId);
    Optional<BillingClerk> findByUser_UserId(Long userId);
    List<BillingClerk> findByHospitalId(String hospitalId);
    List<BillingClerk> findByIsActiveTrue();
}
