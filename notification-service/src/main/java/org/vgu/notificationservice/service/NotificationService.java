package org.vgu.notificationservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.notificationservice.dto.TransactionEvent;
import org.vgu.notificationservice.enums.NotificationChannel;
import org.vgu.notificationservice.enums.NotificationStatus;
import org.vgu.notificationservice.enums.UserType;
import org.vgu.notificationservice.model.Notification;
import org.vgu.notificationservice.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Listen to transaction created events
     */
    @RabbitListener(queues = "transaction.created.queue")
    @Transactional
    public void handleTransactionCreated(TransactionEvent event) {
        log.info("📬 Processing transaction created event: {}", event.getTransactionId());

        try {
            // Create notification record
            Notification notification = createNotification(
                    event.getCustomerId(),
                    UserType.CUSTOMER,
                    "TRANSACTION",
                    "Transaction Submitted",
                    String.format("Your %s transaction of %s %s has been submitted and is pending approval.",
                            event.getTransactionType(), event.getAmount(), event.getCurrency() != null ? event.getCurrency() : "VND"),
                    NotificationChannel.BOTH);

            // Send in-app notification via WebSocket
            sendInAppNotification(event.getCustomerId(), notification);

            // Send email notification
            sendTransactionBookingEmail(event, notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Notification sent for transaction created: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("❌ Error processing transaction created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to transaction approved events
     */
    @RabbitListener(queues = "transaction.approved.queue")
    @Transactional
    public void handleTransactionApproved(TransactionEvent event) {
        log.info("📬 Processing transaction approved event: {}", event.getTransactionId());

        try {
            // Create notification record
            Notification notification = createNotification(
                    event.getCustomerId(),
                    UserType.CUSTOMER,
                    "TRANSACTION",
                    "Transaction Approved",
                    String.format("Your %s transaction of %s %s has been approved and completed successfully.",
                            event.getTransactionType(), event.getAmount(), event.getCurrency() != null ? event.getCurrency() : "VND"),
                    NotificationChannel.BOTH);

            // Send in-app notification via WebSocket
            sendInAppNotification(event.getCustomerId(), notification);

            // Send email notification
            sendTransactionApprovalEmail(event, notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Notification sent for transaction approved: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("❌ Error processing transaction approved event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to transaction rejected events
     */
    @RabbitListener(queues = "transaction.rejected.queue")
    @Transactional
    public void handleTransactionRejected(TransactionEvent event) {
        log.info("📬 Processing transaction rejected event: {}", event.getTransactionId());

        try {
            // Create notification record
            Notification notification = createNotification(
                    event.getCustomerId(),
                    UserType.CUSTOMER,
                    "TRANSACTION",
                    "Transaction Rejected",
                    String.format(
                            "Your %s transaction of %s %s has been rejected. Please contact support for more information.",
                            event.getTransactionType(), event.getAmount(), event.getCurrency() != null ? event.getCurrency() : "VND"),
                    NotificationChannel.BOTH);

            // Send in-app notification via WebSocket
            sendInAppNotification(event.getCustomerId(), notification);

            // Send email notification
            sendTransactionRejectionEmail(event, notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Notification sent for transaction rejected: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("❌ Error processing transaction rejected event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to transaction completed events
     */
    @RabbitListener(queues = "transaction.completed.queue")
    @Transactional
    public void handleTransactionCompleted(TransactionEvent event) {
        log.info("📬 Processing transaction completed event: {}", event.getTransactionId());

        try {
            // Create notification record
            Notification notification = createNotification(
                    event.getCustomerId(),
                    UserType.CUSTOMER,
                    "TRANSACTION",
                    "Transaction Completed",
                    String.format("Your %s transaction of %s %s has been completed successfully.",
                            event.getTransactionType(), event.getAmount(), event.getCurrency() != null ? event.getCurrency() : "VND"),
                    NotificationChannel.BOTH);

            // Send in-app notification via WebSocket
            sendInAppNotification(event.getCustomerId(), notification);

            // Send email notification
            sendTransactionCompletionEmail(event, notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Notification sent for transaction completed: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("❌ Error processing transaction completed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Create notification entity
     */
    private Notification createNotification(
            Long userId,
            UserType userType,
            String type,
            String title,
            String message,
            NotificationChannel channel) {

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setChannel(channel);
        notification.setStatus(NotificationStatus.PENDING);
        return notificationRepository.save(notification);
    }

    /**
     * Send in-app notification via WebSocket
     */
    private void sendInAppNotification(Long userId, Notification notification) {
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            log.info("📤 In-app notification sent to user {} via WebSocket", userId);
        } catch (Exception e) {
            log.error("❌ Error sending in-app notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send transaction booking confirmation email
     */
    private void sendTransactionBookingEmail(TransactionEvent event, Notification notification) {
        try {
            String subject = "Transaction Booking Confirmation";
            String body = buildTransactionBookingEmailBody(event);

            emailService.sendEmail(event.getCustomerEmail(), subject, body);

            notification.setEmailSubject(subject);
            notification.setEmailBody(body);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("📧 Booking confirmation email sent to: {}", event.getCustomerEmail());
        } catch (Exception e) {
            log.error("❌ Error sending booking email: {}", e.getMessage(), e);
        }
    }

    /**
     * Send transaction approval email
     */
    private void sendTransactionApprovalEmail(TransactionEvent event, Notification notification) {
        try {
            String subject = "Transaction Approved Successfully";
            String body = buildTransactionApprovalEmailBody(event);

            emailService.sendEmail(event.getCustomerEmail(), subject, body);

            notification.setEmailSubject(subject);
            notification.setEmailBody(body);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("📧 Approval email sent to: {}", event.getCustomerEmail());
        } catch (Exception e) {
            log.error("❌ Error sending approval email: {}", e.getMessage(), e);
        }
    }

    /**
     * Send transaction rejection email
     */
    private void sendTransactionRejectionEmail(TransactionEvent event, Notification notification) {
        try {
            String subject = "Transaction Rejected";
            String body = buildTransactionRejectionEmailBody(event);

            emailService.sendEmail(event.getCustomerEmail(), subject, body);

            notification.setEmailSubject(subject);
            notification.setEmailBody(body);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("📧 Rejection email sent to: {}", event.getCustomerEmail());
        } catch (Exception e) {
            log.error("❌ Error sending rejection email: {}", e.getMessage(), e);
        }
    }

    /**
     * Build transaction booking email body
     */
    private String buildTransactionBookingEmailBody(TransactionEvent event) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2c3e50;">Transaction Booking Confirmation</h2>
                        <p>Dear %s,</p>
                        <p>Your transaction has been successfully submitted and is pending approval.</p>
                        <div style="background-color: #f4f4f4; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <p><strong>Transaction ID:</strong> %s</p>
                            <p><strong>Type:</strong> %s</p>
                            <p><strong>Amount:</strong> %s %s</p>
                            <p><strong>Status:</strong> Pending Approval</p>
                            <p><strong>Date:</strong> %s</p>
                        </div>
                        <p>You will receive another email once the transaction is approved.</p>
                        <p>If you have any questions, please contact our support team.</p>
                        <p>Best regards,<br>Banking Team</p>
                    </div>
                </body>
                </html>
                """,
                event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                event.getTransactionIdString() != null ? event.getTransactionIdString() : String.valueOf(event.getTransactionId()),
                event.getTransactionType(),
                event.getAmount(),
                event.getCurrency() != null ? event.getCurrency() : "VND",
                event.getCreatedAt() != null ? event.getCreatedAt().toString() : "N/A");
    }

    /**
     * Build transaction approval email body
     */
    private String buildTransactionApprovalEmailBody(TransactionEvent event) {
        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <h2 style="color: #27ae60;">Transaction Approved</h2>
                                <p>Dear %s,</p>
                                <p>Great news! Your transaction has been approved and completed successfully.</p>
                                <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #27ae60;">
                                    <p><strong>Transaction ID:</strong> %s</p>
                                    <p><strong>Type:</strong> %s</p>
                                    <p><strong>Amount:</strong> %s %s</p>
                                    <p><strong>Status:</strong> Approved</p>
                                    <p><strong>Approved Date:</strong> %s</p>
                                </div>
                                <p>Thank you for banking with us!</p>
                                <p>Best regards,<br>Banking Team</p>
                            </div>
                        </body>
                        </html>
                        """,
                event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                event.getTransactionIdString() != null ? event.getTransactionIdString() : String.valueOf(event.getTransactionId()),
                event.getTransactionType(),
                event.getAmount(),
                event.getCurrency() != null ? event.getCurrency() : "VND",
                event.getApprovedAt() != null ? event.getApprovedAt().toString() : "N/A");
    }

    /**
     * Send transaction completion email
     */
    private void sendTransactionCompletionEmail(TransactionEvent event, Notification notification) {
        try {
            String subject = "Transaction Completed Successfully";
            String body = buildTransactionCompletionEmailBody(event);

            emailService.sendEmail(event.getCustomerEmail(), subject, body);

            notification.setEmailSubject(subject);
            notification.setEmailBody(body);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("📧 Completion email sent to: {}", event.getCustomerEmail());
        } catch (Exception e) {
            log.error("❌ Error sending completion email: {}", e.getMessage(), e);
        }
    }

    /**
     * Build transaction rejection email body
     */
    private String buildTransactionRejectionEmailBody(TransactionEvent event) {
        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <h2 style="color: #e74c3c;">Transaction Rejected</h2>
                                <p>Dear %s,</p>
                                <p>We regret to inform you that your transaction has been rejected.</p>
                                <div style="background-color: #f8d7da; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #e74c3c;">
                                    <p><strong>Transaction ID:</strong> %s</p>
                                    <p><strong>Type:</strong> %s</p>
                                    <p><strong>Amount:</strong> %s %s</p>
                                    <p><strong>Status:</strong> Rejected</p>
                                    %s
                                </div>
                                <p>If you have any questions or concerns, please contact our support team.</p>
                                <p>Best regards,<br>Banking Team</p>
                            </div>
                        </body>
                        </html>
                        """,
                event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                event.getTransactionIdString() != null ? event.getTransactionIdString() : String.valueOf(event.getTransactionId()),
                event.getTransactionType(),
                event.getAmount(),
                event.getCurrency() != null ? event.getCurrency() : "VND",
                event.getRejectionReason() != null ? "<p><strong>Reason:</strong> " + event.getRejectionReason() + "</p>" : "");
    }

    /**
     * Build transaction completion email body
     */
    private String buildTransactionCompletionEmailBody(TransactionEvent event) {
        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <h2 style="color: #27ae60;">Transaction Completed</h2>
                                <p>Dear %s,</p>
                                <p>Your transaction has been completed successfully.</p>
                                <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #27ae60;">
                                    <p><strong>Transaction ID:</strong> %s</p>
                                    <p><strong>Type:</strong> %s</p>
                                    <p><strong>Amount:</strong> %s %s</p>
                                    <p><strong>Status:</strong> Completed</p>
                                    <p><strong>Completed Date:</strong> %s</p>
                                </div>
                                <p>Thank you for banking with us!</p>
                                <p>Best regards,<br>Banking Team</p>
                            </div>
                        </body>
                        </html>
                        """,
                event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                event.getTransactionIdString() != null ? event.getTransactionIdString() : String.valueOf(event.getTransactionId()),
                event.getTransactionType(),
                event.getAmount(),
                event.getCurrency() != null ? event.getCurrency() : "VND",
                event.getCreatedAt() != null ? event.getCreatedAt().toString() : "N/A");
    }

    // ========== Public API Methods (for REST Controller) ==========

    /**
     * Create and send notification (used by REST API)
     */
    public Notification createAndSendNotification(Notification notification) {
        log.info("Creating and sending notification for user: {}", notification.getUserId());

        try {
            // Save notification
            notification.setStatus(NotificationStatus.PENDING);
            Notification saved = notificationRepository.save(notification);

            // Send in-app notification if channel includes it
            if (notification.getChannel() == NotificationChannel.IN_APP ||
                    notification.getChannel() == NotificationChannel.BOTH) {
                sendInAppNotification(saved.getUserId(), saved);
            }

            // Send email if channel includes it and email details provided
            if ((notification.getChannel() == NotificationChannel.EMAIL ||
                    notification.getChannel() == NotificationChannel.BOTH) &&
                    notification.getEmailSubject() != null &&
                    notification.getEmailBody() != null) {
                // Email sending would require recipient email address
                // This would need to be fetched from user service or provided in request
                log.info("Email notification requested but recipient email not provided in notification");
            }

            // Update status
            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(LocalDateTime.now());
            return notificationRepository.save(saved);

        } catch (Exception e) {
            log.error("Error creating and sending notification: {}", e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
            return notificationRepository.save(notification);
        }
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications count
     */
    public long getUnreadCount(Long userId) {
        log.info("Fetching unread count for user: {}", userId);
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }
}
