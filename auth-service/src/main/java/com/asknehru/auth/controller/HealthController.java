package com.asknehru.auth.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/actuator/health/readiness")
    public Map<String, Object> readiness() {
        return Map.of(
                "status", "UP",
                "service", "auth-service",
                "timestamp", Instant.now().toString()
        );
    }
}
