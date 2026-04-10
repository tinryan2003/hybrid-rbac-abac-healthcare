package org.vgu.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vgu.notificationservice.model.Announcement;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private Long targetHospitalId;
    private Long targetDepartmentId;
    private Long targetWardId;
    private List<String> targetRoles;
    private String priority;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private String createdByKeycloakId;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static AnnouncementResponse from(Announcement announcement) {
        AnnouncementResponse response = new AnnouncementResponse();
        response.setId(announcement.getId());
        response.setTitle(announcement.getTitle());
        response.setContent(announcement.getContent());
        response.setTargetHospitalId(announcement.getTargetHospitalId());
        response.setTargetDepartmentId(announcement.getTargetDepartmentId());
        response.setTargetWardId(announcement.getTargetWardId());
        
        // Parse JSON targetRoles if present
        if (announcement.getTargetRoles() != null && !announcement.getTargetRoles().isBlank()) {
            try {
                response.setTargetRoles(
                    com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(announcement.getTargetRoles(), List.class)
                );
            } catch (Exception e) {
                response.setTargetRoles(null);
            }
        }
        
        response.setPriority(announcement.getPriority().name());
        response.setStatus(announcement.getStatus().name());
        response.setPublishedAt(announcement.getPublishedAt());
        response.setExpiresAt(announcement.getExpiresAt());
        response.setCreatedByKeycloakId(announcement.getCreatedByKeycloakId());
        response.setCreatedByName(announcement.getCreatedByName());
        response.setCreatedAt(announcement.getCreatedAt());
        response.setUpdatedAt(announcement.getUpdatedAt());
        
        return response;
    }
}
