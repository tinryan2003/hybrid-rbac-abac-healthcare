package org.vgu.labservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.labservice.model.LabOrderItem;

import java.util.List;

@Repository
public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, Long> {
    List<LabOrderItem> findByLabOrder_LabOrderId(Long labOrderId);
    List<LabOrderItem> findByStatus(String status);
}
