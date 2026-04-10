package org.vgu.springcloudgateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway health and info endpoint
 */
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "banking-gateway");
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Banking API Gateway");
        info.put("description", "Spring Cloud Gateway with Keycloak Integration");
        info.put("version", "1.0.0");
        info.put("features", new String[] {
                "JWT Authentication",
                "RBAC Authorization",
                "Rate Limiting",
                "Circuit Breaker",
                "Request Logging"
        });

        return ResponseEntity.ok(info);
    }
}
