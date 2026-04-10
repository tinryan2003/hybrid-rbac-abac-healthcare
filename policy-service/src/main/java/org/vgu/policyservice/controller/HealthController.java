package org.vgu.policyservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/policies")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("[POLICY] GET /policies/health");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "policy-service");
        return ResponseEntity.ok(response);
    }
}
