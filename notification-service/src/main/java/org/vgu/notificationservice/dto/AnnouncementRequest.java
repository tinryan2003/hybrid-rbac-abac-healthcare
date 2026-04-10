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
public class AnnouncementRequest {
    private String title;
    private String content;
    private Long targetHospitalId;
    private Long targetDepartmentId;
    private Long targetWardId;
    private List<String> targetRoles; // ["DOCTOR","NURSE"] or null for all
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private String status;   // DRAFT, PUBLISHED, ARCHIVED
    private LocalDateTime expiresAt;
}
