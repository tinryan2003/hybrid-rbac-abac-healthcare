package org.vgu.pharmacyservice.repository;

import org.vgu.pharmacyservice.model.MedicineInventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineInventoryTransactionRepository extends JpaRepository<MedicineInventoryTransaction, Long> {
    List<MedicineInventoryTransaction> findByMedicine_MedicineIdOrderByTransactionDateDesc(Long medicineId);
    List<MedicineInventoryTransaction> findByTransactionType(String transactionType);
    List<MedicineInventoryTransaction> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);
    void deleteByMedicine_MedicineId(Long medicineId);
}
