package org.vgu.notificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.vgu.notificationservice.dto.AnnouncementRequest;
import org.vgu.notificationservice.dto.AnnouncementResponse;
import org.vgu.notificationservice.service.AnnouncementService;

import java.util.HashMap;
import java.util.Map;

/**
 * Announcement Controller - Hospital-wide announcements (ADMIN creates, everyone reads)
 * 
 * Endpoints:
 * - GET /api/announcements - list published announcements (public)
 * - GET /api/announcements/all - list all (admin: includes drafts/archived)
 * - GET /api/announcements/{id} - get by id
 * - POST /api/announcements - create (admin only)
 * - PUT /api/announcements/{id} - update (admin only)
 * - DELETE /api/announcements/{id} - delete (admin only)
 * - POST /api/announcements/{id}/publish - publish draft (admin only)
 */
@Slf4j
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    
    private final AnnouncementService announcementService;
    
    /**
     * Get all published announcements (public endpoint for all authenticated users)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.info("GET /api/announcements page={} size={}", page, size);
        
        Sort sort = sortDirection.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AnnouncementResponse> announcements = announcementService.getPublishedAnnouncements(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", announcements.getContent());
        response.put("currentPage", announcements.getNumber());
        response.put("totalPages", announcements.getTotalPages());
        response.put("totalItems", announcements.getTotalElements());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all announcements (admin only: includes drafts/archived)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("GET /api/announcements/all (admin)");
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AnnouncementResponse> announcements = announcementService.getAllAnnouncements(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", announcements.getContent());
        response.put("currentPage", announcements.getNumber());
        response.put("totalPages", announcements.getTotalPages());
        response.put("totalItems", announcements.getTotalElements());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get announcement by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        log.info("GET /api/announcements/{}", id);
        
        AnnouncementResponse announcement = announcementService.getById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", announcement);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a new announcement (admin only)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication) {
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();
        String creatorName = jwt.getClaimAsString("name");
        if (creatorName == null || creatorName.isBlank()) {
            creatorName = jwt.getClaimAsString("preferred_username");
        }
        
        log.info("POST /api/announcements by {}", keycloakId);
        
        AnnouncementResponse created = announcementService.create(request, keycloakId, creatorName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Announcement created successfully");
        response.put("data", created);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Update an existing announcement (admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request) {
        
        log.info("PUT /api/announcements/{}", id);
        
        AnnouncementResponse updated = announcementService.update(id, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Announcement updated successfully");
        response.put("data", updated);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete announcement (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        log.info("DELETE /api/announcements/{}", id);
        
        announcementService.delete(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Announcement deleted successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Publish a draft announcement (admin only)
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> publish(@PathVariable Long id) {
        log.info("POST /api/announcements/{}/publish", id);
        
        AnnouncementResponse published = announcementService.publish(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Announcement published successfully");
        response.put("data", published);
        
        return ResponseEntity.ok(response);
    }
}
