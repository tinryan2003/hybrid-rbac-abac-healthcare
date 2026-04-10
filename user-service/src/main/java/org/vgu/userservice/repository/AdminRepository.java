package org.vgu.userservice.repository;

import org.vgu.userservice.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUser_KeycloakUserId(String keycloakUserId);
    Optional<Admin> findByUser_UserId(Long userId);
    List<Admin> findByHospitalId(String hospitalId);
    List<Admin> findByAdminLevel(Admin.AdminLevel adminLevel);
}
