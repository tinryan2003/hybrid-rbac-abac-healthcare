package org.vgu.notificationservice.dto;

import org.vgu.notificationservice.enums.NotificationChannel;
import org.vgu.notificationservice.enums.UserType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private UserType userType;
    private String type; // TRANSACTION, APPROVAL, etc.
    private String title;
    private String message;
    private String emailSubject;
    private String emailBody; // HTML email body
    private NotificationChannel channel = NotificationChannel.BOTH;
    private String metadata; // JSON string for additional data
}
