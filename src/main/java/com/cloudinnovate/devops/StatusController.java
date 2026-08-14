package com.cloudinnovate.devops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatusController {

    @Value("${DEPLOYMENT_ENV:local}")
    private String environment;

    @Value("${DEPLOYMENT_PLATFORM:standalone}")
    private String deployment;

    @GetMapping("/api/status")
    public Map<String, String> status() {
        return Map.of(
                "application", "devops-status-app",
                "status", "UP",
                "environment", environment,
                "deployment", deployment
        );
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP"
        );
    }
}
