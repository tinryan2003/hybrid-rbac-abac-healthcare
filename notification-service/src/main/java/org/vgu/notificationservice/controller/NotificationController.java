package org.vgu.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.vgu.notificationservice.dto.NotificationRequest;
import org.vgu.notificationservice.model.Notification;
import org.vgu.notificationservice.service.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Create and send a notification
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody NotificationRequest request) {
        log.info("Creating notification for user: {}", request.getUserId());
        
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setUserType(request.getUserType());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setEmailSubject(request.getEmailSubject());
        notification.setEmailBody(request.getEmailBody());
        notification.setChannel(request.getChannel());
        notification.setMetadata(request.getMetadata());
        
        Notification saved = notificationService.createAndSendNotification(notification);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification created and sent successfully");
        response.put("data", saved);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get notifications for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", notifications);
        response.put("count", notifications.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get unread notifications count
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("unreadCount", count);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification marked as read");
        
        return ResponseEntity.ok(response);
    }
}

