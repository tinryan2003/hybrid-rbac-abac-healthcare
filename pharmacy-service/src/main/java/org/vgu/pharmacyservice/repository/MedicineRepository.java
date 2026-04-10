package org.vgu.pharmacyservice.repository;

import org.vgu.pharmacyservice.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByHospitalId(String hospitalId);
    List<Medicine> findByIsActiveTrue();
    List<Medicine> findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(String name, String genericName);
    List<Medicine> findByCategory(String category);
    Optional<Medicine> findByNameAndHospitalId(String name, String hospitalId);
    List<Medicine> findByStockQuantityLessThanEqual(Integer reorderLevel);
}
