package org.vgu.notificationservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vgu.notificationservice.model.Announcement;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    
    /**
     * Find all published announcements (not expired)
     */
    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.publishedAt DESC")
    Page<Announcement> findPublishedAnnouncements(@Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Find announcements by status
     */
    Page<Announcement> findByStatusOrderByCreatedAtDesc(Announcement.Status status, Pageable pageable);
    
    /**
     * Find announcements created by specific user
     */
    Page<Announcement> findByCreatedByKeycloakIdOrderByCreatedAtDesc(String keycloakId, Pageable pageable);
    
    /**
     * Find announcements by hospital/department/ward (for filtering)
     */
    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
           "AND (a.targetHospitalId IS NULL OR a.targetHospitalId = :hospitalId) " +
           "AND (a.targetDepartmentId IS NULL OR a.targetDepartmentId = :departmentId) " +
           "ORDER BY a.publishedAt DESC")
    List<Announcement> findByTargetFilters(
        @Param("hospitalId") Long hospitalId,
        @Param("departmentId") Long departmentId
    );
}
