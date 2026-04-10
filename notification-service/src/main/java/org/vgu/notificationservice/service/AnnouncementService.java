package org.vgu.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.notificationservice.dto.AnnouncementRequest;
import org.vgu.notificationservice.dto.AnnouncementResponse;
import org.vgu.notificationservice.model.Announcement;
import org.vgu.notificationservice.repository.AnnouncementRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {
    
    private final AnnouncementRepository announcementRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Get all published announcements (public, not expired)
     */
    public Page<AnnouncementResponse> getPublishedAnnouncements(Pageable pageable) {
        log.info("Fetching published announcements");
        Page<Announcement> announcements = announcementRepository
            .findPublishedAnnouncements(LocalDateTime.now(), pageable);
        return announcements.map(AnnouncementResponse::from);
    }
    
    /**
     * Get all announcements (admin view: includes drafts/archived)
     */
    public Page<AnnouncementResponse> getAllAnnouncements(Pageable pageable) {
        log.info("Fetching all announcements (admin)");
        Page<Announcement> announcements = announcementRepository.findAll(pageable);
        return announcements.map(AnnouncementResponse::from);
    }
    
    /**
     * Get announcement by ID
     */
    public AnnouncementResponse getById(Long id) {
        log.info("Fetching announcement id={}", id);
        Announcement announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        return AnnouncementResponse.from(announcement);
    }
    
    /**
     * Create a new announcement
     * @param request announcement data
     * @param keycloakId creator's Keycloak ID
     * @param creatorName creator's display name (from JWT or user service)
     */
    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request, String keycloakId, String creatorName) {
        log.info("Creating announcement: title={}, createdBy={}", request.getTitle(), keycloakId);
        
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setTargetHospitalId(request.getTargetHospitalId());
        announcement.setTargetDepartmentId(request.getTargetDepartmentId());
        announcement.setTargetWardId(request.getTargetWardId());
        
        // Serialize targetRoles to JSON
        if (request.getTargetRoles() != null && !request.getTargetRoles().isEmpty()) {
            try {
                announcement.setTargetRoles(objectMapper.writeValueAsString(request.getTargetRoles()));
            } catch (Exception e) {
                log.warn("Failed to serialize targetRoles, skipping");
            }
        }
        
        announcement.setPriority(
            request.getPriority() != null 
                ? Announcement.Priority.valueOf(request.getPriority().toUpperCase())
                : Announcement.Priority.MEDIUM
        );
        
        announcement.setStatus(
            request.getStatus() != null
                ? Announcement.Status.valueOf(request.getStatus().toUpperCase())
                : Announcement.Status.DRAFT
        );
        
        announcement.setExpiresAt(request.getExpiresAt());
        announcement.setCreatedByKeycloakId(keycloakId);
        announcement.setCreatedByName(creatorName);
        
        // Auto-set publishedAt if status is PUBLISHED
        if (announcement.getStatus() == Announcement.Status.PUBLISHED && announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
        
        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement created: id={}", saved.getId());
        
        return AnnouncementResponse.from(saved);
    }
    
    /**
     * Update an existing announcement
     */
    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementRequest request) {
        log.info("Updating announcement id={}", id);
        
        Announcement announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setTargetHospitalId(request.getTargetHospitalId());
        announcement.setTargetDepartmentId(request.getTargetDepartmentId());
        announcement.setTargetWardId(request.getTargetWardId());
        
        if (request.getTargetRoles() != null) {
            try {
                announcement.setTargetRoles(objectMapper.writeValueAsString(request.getTargetRoles()));
            } catch (Exception e) {
                log.warn("Failed to serialize targetRoles, keeping old value");
            }
        }
        
        if (request.getPriority() != null) {
            announcement.setPriority(Announcement.Priority.valueOf(request.getPriority().toUpperCase()));
        }
        
        if (request.getStatus() != null) {
            Announcement.Status newStatus = Announcement.Status.valueOf(request.getStatus().toUpperCase());
            Announcement.Status oldStatus = announcement.getStatus();
            
            // Auto-set publishedAt when transitioning DRAFT→PUBLISHED
            if (oldStatus == Announcement.Status.DRAFT && newStatus == Announcement.Status.PUBLISHED) {
                announcement.setPublishedAt(LocalDateTime.now());
            }
            
            announcement.setStatus(newStatus);
        }
        
        announcement.setExpiresAt(request.getExpiresAt());
        
        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement updated: id={}", id);
        
        return AnnouncementResponse.from(updated);
    }
    
    /**
     * Delete announcement (admin only)
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting announcement id={}", id);
        if (!announcementRepository.existsById(id)) {
            throw new RuntimeException("Announcement not found: " + id);
        }
        announcementRepository.deleteById(id);
        log.info("Announcement deleted: id={}", id);
    }
    
    /**
     * Publish a draft announcement (change status to PUBLISHED)
     */
    @Transactional
    public AnnouncementResponse publish(Long id) {
        log.info("Publishing announcement id={}", id);
        
        Announcement announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        
        if (announcement.getStatus() == Announcement.Status.PUBLISHED) {
            throw new RuntimeException("Announcement already published");
        }
        
        announcement.setStatus(Announcement.Status.PUBLISHED);
        announcement.setPublishedAt(LocalDateTime.now());
        
        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement published: id={}", id);
        
        return AnnouncementResponse.from(updated);
    }
}
