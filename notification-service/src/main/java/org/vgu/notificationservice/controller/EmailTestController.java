package org.vgu.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.vgu.notificationservice.service.EmailService;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for email functionality
 * Remove or secure this in production!
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class EmailTestController {

    private final EmailService emailService;

    /**
     * Test email sending
     * Usage: GET http://localhost:8087/api/test/email?to=recipient@gmail.com
     */
    @GetMapping("/email")
    public Map<String, String> testEmail(@RequestParam String to) {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Testing email to: {}", to);
            
            String subject = "🔔 Test Email from VGU Banking System";
            String body = buildTestEmailBody();
            
            emailService.sendEmail(to, subject, body);
            
            response.put("status", "success");
            response.put("message", "Test email sent successfully to " + to);
            response.put("info", "Check your inbox (and spam folder)");
            
            log.info("Test email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", to, e);
            response.put("status", "error");
            response.put("message", "Failed to send email: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return response;
    }

    private String buildTestEmailBody() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        padding: 20px;
                        text-align: center;
                        border-radius: 8px 8px 0 0;
                    }
                    .content {
                        background: #f9f9f9;
                        padding: 30px;
                        border-radius: 0 0 8px 8px;
                    }
                    .success {
                        background: #4CAF50;
                        color: white;
                        padding: 15px;
                        border-radius: 5px;
                        margin: 20px 0;
                        text-align: center;
                    }
                    .info {
                        background: #e3f2fd;
                        border-left: 4px solid #2196F3;
                        padding: 15px;
                        margin: 20px 0;
                    }
                    .footer {
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #ddd;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>🎉 Email Configuration Test</h1>
                    <p>VGU Banking System - Notification Service</p>
                </div>
                
                <div class="content">
                    <div class="success">
                        <h2>✅ Email Configuration Successful!</h2>
                        <p>Your SMTP settings are working correctly.</p>
                    </div>
                    
                    <h3>Test Details:</h3>
                    <div class="info">
                        <p><strong>Service:</strong> Notification Service</p>
                        <p><strong>Port:</strong> 8087</p>
                        <p><strong>SMTP:</strong> Gmail (smtp.gmail.com:587)</p>
                        <p><strong>Protocol:</strong> STARTTLS</p>
                        <p><strong>Time:</strong> """ + java.time.LocalDateTime.now() + """
                        </p>
                    </div>
                    
                    <h3>What's Next?</h3>
                    <ul>
                        <li>✅ SMTP configuration is working</li>
                        <li>📧 Email notifications are ready</li>
                        <li>🔔 WebSocket notifications are configured</li>
                        <li>🐰 RabbitMQ integration is set up</li>
                    </ul>
                    
                    <p><strong>You can now:</strong></p>
                    <ol>
                        <li>Integrate with Transaction Service</li>
                        <li>Start sending real notifications</li>
                        <li>Test end-to-end notification flow</li>
                    </ol>
                </div>
                
                <div class="footer">
                    <p>VGU Banking System - Notification Service</p>
                    <p>This is an automated test email. Please do not reply.</p>
                </div>
            </body>
            </html>
            """;
    }
}

