package org.vgu.labservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.labservice.model.LabTestCatalog;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabTestCatalogRepository extends JpaRepository<LabTestCatalog, Long> {
    Optional<LabTestCatalog> findByTestCode(String testCode);
    List<LabTestCatalog> findByTestCategory(String testCategory);
    List<LabTestCatalog> findByIsActive(Boolean isActive);
    List<LabTestCatalog> findByIsActiveTrue();
    List<LabTestCatalog> findByTestCategoryAndIsActiveTrue(String testCategory);
}
