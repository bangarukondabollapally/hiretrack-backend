package com.hiretrack.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * HealthController — TASK-001.
 *
 * Exposes GET /api/health as a public (no-auth) endpoint.
 * Response: 200 OK with body { "status": "UP" } per the decision recorded
 * in the planning report (B1) and reflected in docs/API.md.
 *
 * This endpoint is distinct from Spring Actuator (/actuator/health), which
 * will be added in TASK-033. This one serves as a simple "is the API alive?"
 * check accessible to external callers and the frontend without a token.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
