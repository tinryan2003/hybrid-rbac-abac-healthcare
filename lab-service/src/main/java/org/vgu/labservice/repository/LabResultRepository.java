package org.vgu.labservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.labservice.model.LabResult;

import java.util.List;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    List<LabResult> findByLabOrderId(Long labOrderId);
    List<LabResult> findByOrderItemId(Long orderItemId);
    List<LabResult> findByResultStatus(String resultStatus);
}
