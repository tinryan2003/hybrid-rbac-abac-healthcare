package org.vgu.userservice.repository;

import org.vgu.userservice.model.Receptionist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceptionistRepository extends JpaRepository<Receptionist, Long> {
    
    Optional<Receptionist> findByUser_KeycloakUserId(String keycloakUserId);
    Optional<Receptionist> findByUser_UserId(Long userId);
    List<Receptionist> findByHospitalId(String hospitalId);
    List<Receptionist> findByIsActiveTrue();
}
