package org.vgu.billingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.billingservice.model.ServicePricing;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePricingRepository extends JpaRepository<ServicePricing, Long> {
    Optional<ServicePricing> findByServiceCode(String serviceCode);
    List<ServicePricing> findByServiceCategory(String serviceCategory);
    List<ServicePricing> findByIsActive(Boolean isActive);
    List<ServicePricing> findByHospitalId(String hospitalId);
}
